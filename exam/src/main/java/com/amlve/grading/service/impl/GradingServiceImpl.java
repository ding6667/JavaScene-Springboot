package com.amlve.grading.service.impl;

import com.amlve.grading.bean.DifyClient;
import com.amlve.grading.dto.request.ContentRequest;
import com.amlve.grading.dto.request.RubricRequest;
import com.amlve.dialogueapi.dto.api.StudentRagInsertDTO;
import com.amlve.grading.service.GradingService;
import com.amlve.grading.constant.BusinessConstant;
import com.amlve.grading.constant.ErrorCodeConstant;
import com.amlve.grading.constant.ErrorMessageConstant;
import com.amlve.grading.entity.StudentHomework;
import com.amlve.dialogueapi.entity.CommentJson;
import com.amlve.dialogueapi.service.RagService;
import com.amlve.grading.tableEnt.GradingTask;
import com.amlve.grading.enumeration.GradingTaskStatus;
import com.amlve.grading.enumeration.FileType;
import com.amlve.grading.enumeration.TaskCategory;
import com.amlve.systemapi.dto.api.StandardConfigDTO;
import com.amlve.fileapi.dto.api.UploadFileDTO;
import com.amlve.common.entity.RubricItem;
import com.amlve.common.exception.BusinessException;
import com.amlve.grading.properties.DifyProperty;
import com.amlve.systemapi.service.StandardService;
import com.amlve.fileapi.service.UploadService;
import com.amlve.grading.tableEnt.StudentResult;
import com.amlve.common.utils.FileFormatUtil;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.common.utils.ZipUtil;
import com.amlve.grading.mapper.GradingTaskMapper;
import com.amlve.grading.mapper.StudentResultMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Service
@RequiredArgsConstructor
public class GradingServiceImpl implements GradingService {

    private final ObjectMapper objectMapper;
    private final RagService ragService;
    @Value("${temp.unzip}")
    private String tempUnzipDir;
    @Value("${temp.expire_time}")
    private String expireTime;

    private final DifyProperty difyProperty;
    private final DifyClient difyClient;
    private final GradingTaskMapper gradingTaskMapper;
    private final StudentResultMapper studentResultMapper;
    private final StandardService standardConfigService;
    private final UploadService uploadService;
    private final FileFormatUtil fileFormatUtil;


    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private static final Semaphore CONVERT_SEM = new Semaphore(Math.max(1, Runtime.getRuntime().availableProcessors()));

    private final ObjectMapper mapper = new ObjectMapper();

    // 匹配学号和姓名
    private static final Pattern STUDENT_INFO_PATTERN = Pattern.compile("^(\\d{10})(\\s*)([\\u4e00-\\u9fa5·]{2,16})");

    private final ConcurrentMap<String, TaskContext> activeTasks = new ConcurrentHashMap<>();


    /**
     * 容器关闭时关闭虚拟线程执行器
     */
    @PreDestroy
    public void destroy() {
        log.info("正在关闭虚拟线程执行器...");
        virtualExecutor.close();
        log.info("虚拟线程执行器已关闭");
    }

    /**
     * 单个批阅任务的运行时上下文，每个任务独立持有自己的锁、状态、学生列表
     */
    private static class TaskContext {
        final String taskId;
        List<RubricItem> rubric;
        List<StudentResult> students;
        final ReentrantLock pauseLock = new ReentrantLock();
        final Condition pausedCondition = pauseLock.newCondition();

        volatile boolean paused = false;
        volatile boolean cancelled = false;
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        Future<?> future;
        TaskContext(String taskId) {
            this.taskId = taskId;
        }
    }

    //========================================================批量批阅word文件==========================================================================

    /**
     * 异步
     * @param uploadIds 上传的文件ID列表
     * @param standardConfigId 评分标准ID
     * @param classId 班级ID
     * @param taskCategory 任务类别
     * @param taskSequence 任务频率序号
     */
    @Override
    public void runWorkflowWithCommonFiles(List<String> uploadIds, Long standardConfigId, String classId, TaskCategory taskCategory, Integer taskSequence) {
        if (uploadIds.isEmpty()) {
            throw new BusinessException(ErrorMessageConstant.FILES_NOT_FOUND);
        }
        // TODO 需处理result.code == error 异常情况

        List<RubricItem> rubric = standardConfigService.selectById(standardConfigId).getData().getStandardContent();

        List<Callable<Void>> tasks = uploadIds.stream()
                .map(uploadId -> (Callable<Void>) () -> {
                    // 这个方法体将在虚拟线程中执行
                    processSingleUpload(
                            uploadId,
                            standardConfigId,
                            classId,
                            taskCategory,
                            taskSequence,
                            rubric
                    );
                    return null;
                })
                .toList();
        try {
            List<Future<Void>> futures = virtualExecutor.invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get();
            }

        } catch (InterruptedException e) {
            // 主线程被中断（例如服务关闭时）
            Thread.currentThread().interrupt();
            log.warn("批量评分任务被中断");
            throw new BusinessException(ErrorMessageConstant.TASK_CANCELLED);

        } catch (ExecutionException e) {
            // 子任务中的业务异常被包装后抛出，解包并抛出原始异常
            Throwable cause = e.getCause();
            log.error("批量评分任务执行失败", cause);
            if (cause instanceof BusinessException) {
                throw (BusinessException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new BusinessException(ErrorCodeConstant.SYSTEM_ERROR, cause.getMessage());
        }
        log.info("所有文件处理完成，总文件数: {}", uploadIds.size());
    }
    /**
     * 处理单个上传文件（全程在虚拟线程中同步执行）
     * 流程：转换 -> 入库 -> 调用 Dify -> 保存结果
     */
    private void processSingleUpload(String uploadId, Long standardConfigId,
                                     String classId, TaskCategory taskCategory,
                                     Integer taskSequence, List<RubricItem> rubric) {

        String taskId = null;
        StudentHomework studentHomework = null;


        try {
            // 获取许可，如果当前并发转换数已达到 CPU 核心数，则阻塞等待
            CONVERT_SEM.acquire();

            // 真正执行转换（同步阻塞）
            try {
                // 查询上传文件信息
                UploadFileDTO uploadFile = uploadService.selectById(uploadId);
                if (!FileType.fromCode(uploadFile.getFileType()).equals(FileType.WORD)) {
                    throw new BusinessException(ErrorMessageConstant.FILE_TYPE_INVALID);
                }

                // 调用工具类将 Word 转为 MD
                Map<String, List<String>> mdMap = fileFormatUtil.word2Md(
                        List.of(uploadFile.getRawStoragePath())
                );
                String content = mdMap.get(BusinessConstant.FILES).get(0);

                // 解析文件名获取学生信息
                String[] studentInfoParts = parseStudentInfo(uploadFile.getFileName());
                if (studentInfoParts == null || studentInfoParts.length != 2) {
                    throw new BusinessException(ErrorMessageConstant.FILE_NAME_INVALID);
                }
                String studentId = studentInfoParts[0];
                String studentName = studentInfoParts[1];

                // 生成任务 ID 并插入 GradingTask 初始记录
                taskId = UUID.randomUUID().toString();
                GradingTask task = new GradingTask();
                task.setId(taskId);
                task.setFileId(uploadId);
                task.setUserId(SecurityUtils.getUserId());
                task.setRubricId(standardConfigId);
                task.setStatus(GradingTaskStatus.PENDING);
                task.setCreateTime(LocalDateTime.now());
                task.setStudentCount(1);
                task.setProcessedStudentCount(0);
                task.setClassId(classId);
                task.setTaskCategory(taskCategory);
                task.setTaskSequence(taskSequence);
                task.setFileType(FileType.WORD);
                task.setStudentId(studentId);
                task.setSemester(extractSemester(studentId));
                gradingTaskMapper.insert(task);

                // 5. 构造内存中的学生作业对象
                studentHomework = new StudentHomework(
                        studentId,
                        studentName,
                        content,
                        uploadFile.getRawStoragePath()
                );

                log.debug("文件 {} 转换完成，taskId: {}", uploadId, taskId);

            } finally {
                // 无论转换成功还是失败，都必须释放信号量许可，让其他等待的虚拟线程有机会执行
                CONVERT_SEM.release();
            }

        } catch (InterruptedException e) {
            // 获取信号量时被中断（取消操作）
            Thread.currentThread().interrupt();
            log.warn("文件 {} 转换被中断", uploadId);
            throw new RuntimeException(ErrorMessageConstant.TASK_CANCELLED, e);
        }

        // 如果转换阶段失败（抛异常），taskId 为 null，直接返回外层捕获
        if (taskId == null || studentHomework == null) {
            // 正常情况下不会走到这里，因为转换失败会抛出异常
            return;
        }


        // 创建并维护任务上下文（用于外部的暂停/取消控制）
        TaskContext taskContext = new TaskContext(taskId);
        taskContext.rubric = rubric; // 共享评分标准
        activeTasks.put(taskId, taskContext);

        try {
            // 检查是否在启动前已被取消
            if (taskContext.cancelled) {
                throw new InterruptedException(ErrorMessageConstant.TASK_CANCELLED);
            }

            // 更新任务状态为处理中
            GradingTask gradingTask = gradingTaskMapper.selectById(taskId);
            gradingTask.setStatus(GradingTaskStatus.PROCESSING);
            gradingTask.setProcessedStudentCount(1);
            gradingTaskMapper.updateById(gradingTask);

            // 处理暂停逻辑
            if (taskContext.paused) {
                taskContext.pauseLock.lock();
                try {
                    taskContext.pausedCondition.await();
                } finally {
                    taskContext.pauseLock.unlock();
                }
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put(BusinessConstant.RUBRIC, new RubricRequest(rubric));
            requestBody.put(BusinessConstant.CONTENT, new ContentRequest(List.of(studentHomework)));
            requestBody.put(BusinessConstant.HANDLE_TYPE, BusinessConstant.COMMON);

            String result = difyClient.runWorkflowBlocking(difyProperty.getApiKey(), requestBody);

            // 解析 Dify 返回的 JSON
            String outputJson = extractOutputAsJson(result);
            if (outputJson == null) {
                throw new BusinessException(ErrorCodeConstant.SYSTEM_ERROR, ErrorMessageConstant.GRADING_TASK_EXCEPTION_TERMINATED);
            }

            JsonNode root = mapper.readTree(outputJson);
            JsonNode outputArray = root.get("output");
            if (outputArray == null || !outputArray.isArray()) {
                throw new BusinessException(ErrorCodeConstant.SYSTEM_ERROR, ErrorMessageConstant.GRADING_TASK_EXCEPTION_TERMINATED);
            }
            String totalScore = "";
            String review = "";
            for (JsonNode studentNode : outputArray) {
                totalScore = studentNode.path("total_score").asText("");
                review = studentNode.path("content").asText("");
                // 如果数组只有一个元素，循环一次后退出即可
            }

            // 构建并保存 StudentResult
            StudentResult studentResult = StudentResult.builder()
                    .taskId(taskId)
                    .studentId(studentHomework.getStudentId())
                    .studentName(studentHomework.getStudentName())
                    .rawScore(totalScore)
                    .aiComment(review)
                    .status(GradingTaskStatus.SUCCESS)
                    .errorMessage(null)
                    .wordContentPath(studentHomework.getPath())
                    .deleted(0)
                    .build();
            saveProgress(List.of(studentResult), taskId);

            updateGradingTaskStatus(taskId, GradingTaskStatus.SUCCESS);

            log.info("任务 {} 处理成功", taskId);

        } catch (InterruptedException e) {
            // 处理中断（来自取消或暂停时的 await 被中断）
            Thread.currentThread().interrupt(); // 恢复中断状态
            log.warn("任务 {} 被中断", taskId);
            GradingTask gradingTask = gradingTaskMapper.selectById(taskId);
            if (gradingTask != null) {
                gradingTask.setStatus(GradingTaskStatus.CANCELLED);
                gradingTask.setErrorMessage(ErrorMessageConstant.TASK_CANCELLED);
                gradingTaskMapper.updateById(gradingTask);
            }
            // 向上抛出，让 invokeAll 捕获到异常
            throw new RuntimeException(ErrorMessageConstant.TASK_CANCELLED, e);

        } catch (Exception e) {
            // 其他业务异常（Dify 调用失败、JSON 解析失败等）
            log.error("任务 {} 处理失败", taskId, e);
            GradingTask gradingTask = gradingTaskMapper.selectById(taskId);
            if (gradingTask != null) {
                gradingTask.setStatus(GradingTaskStatus.FAILED);
                gradingTask.setErrorMessage(ErrorMessageConstant.GRADING_TASK_EXCEPTION_TERMINATED);
                gradingTaskMapper.updateById(gradingTask);
            }
            // 向上抛出业务异常
            throw new BusinessException(ErrorCodeConstant.UNKNOWN_ERROR,
                    ErrorMessageConstant.GRADING_TASK_EXCEPTION_TERMINATED, e);

        } finally {
            // 无论成功、失败、取消，最终都要从活跃任务 Map 中移除
            activeTasks.remove(taskId);
        }
    }

    //========================================================单次批阅zip文件==========================================================================

    /**
     * 启动批阅
     * HTTP 线程生成 taskId、插入任务记录（PENDING）、提交到线程池，然后立刻返回。
     * 解压、创建学生记录、批阅全部在线程池中异步执行。
     */
    @Override
    public String startBatchReviewFromZip(String uploadId, Long standardConfigId, String classId, TaskCategory taskCategory, Integer taskSequence) {
        UploadFileDTO uploadFile = uploadService.selectById(uploadId);
        if (!FileType.fromCode(uploadFile.getFileType()).equals(FileType.ZIP)) {
            throw new BusinessException(ErrorMessageConstant.FILE_TYPE_INVALID);
        }
        if (!uploadFile.getFileName().endsWith(".zip")) {
            throw new BusinessException(ErrorMessageConstant.FILE_NAME_INVALID);
        }
        String taskId = UUID.randomUUID().toString();

        GradingTask task = new GradingTask();
        task.setId(taskId);
        task.setFileId(uploadId);
        task.setUserId(SecurityUtils.getUserId());
        task.setRubricId(standardConfigId);
        task.setStatus(GradingTaskStatus.PENDING);
        task.setCreateTime(LocalDateTime.now());
        task.setProcessedStudentCount(0);
        task.setClassId(classId);
        task.setTaskCategory(taskCategory);
        task.setTaskSequence(taskSequence);
        task.setFileType(FileType.ZIP);

        gradingTaskMapper.insert(task);

        TaskContext ctx = new TaskContext(taskId);

        ctx.future = virtualExecutor.submit(() -> {
            try {
                // 获取评分标准
                List<RubricItem> rubric = null;
                // TODO 需处理result.code == error 异常情况
                if (standardConfigId != null) {
                    StandardConfigDTO config = standardConfigService.selectById(standardConfigId).getData();
                    if (config != null) {
                        rubric = config.getStandardContent();
                    }
                }

                // 解压总 zip，提取学生作业

                List<StudentHomework> homeworks = extractFromTotalZip(taskId,uploadId);

                if (homeworks.isEmpty()) {
                    throw new BusinessException(ErrorCodeConstant.FILE_NOT_FOUND, ErrorMessageConstant.FILES_NOT_FOUND);
                }

                // 从第一个学生学号中提取学期信息并更新任务
                String firstStudentId = homeworks.get(0).getStudentId();
                task.setSemester(extractSemester(firstStudentId));
                gradingTaskMapper.updateById(task);

                // 创建 StudentResult 记录
                List<StudentResult> students = new ArrayList<>();
                for (StudentHomework hw : homeworks) {
                    StudentResult sr = StudentResult.builder()
                            .taskId(taskId)
                            .studentId(hw.getStudentId())
                            .studentName(hw.getStudentName())
                            .status(GradingTaskStatus.PENDING)
                            .wordContentPath(hw.getPath())
                            .build();
                    studentResultMapper.insert(sr);
                    students.add(sr);
                }

                // 更新任务状态为处理中，写入学生总数
                task.setStudentCount(students.size());
                task.setStatus(GradingTaskStatus.PROCESSING);
                gradingTaskMapper.updateById(task);

                ctx.students = students;
                ctx.rubric = rubric;

                activeTasks.put(taskId, ctx);
                runBatchReviewScheduler(ctx);

            } catch (Exception e) {
                log.error("批阅任务执行失败: taskId={}, {}", taskId, e.getMessage(), e);
                task.setStatus(GradingTaskStatus.FAILED);
                task.setErrorMessage(e.getMessage());
                task.setFinishTime(LocalDateTime.now());
                gradingTaskMapper.updateById(task);
            }
        });

        return taskId;
    }
    private void runBatchReviewScheduler(TaskContext ctx) {
        List<StudentResult> students = ctx.students;
        int totalStudents = students.size();
        int totalBatches = (totalStudents + 4) / 5;
        log.info("开始批阅: taskId={}, 总学生数={}, 总批次数={}", ctx.taskId, totalStudents, totalBatches);

        List<Callable<Void>> batchTasks = new ArrayList<>();
        for (int batchIdx = 0; batchIdx < totalBatches; batchIdx++) {
            final int index = batchIdx;
            batchTasks.add(() -> {
                // 检查取消和暂停
                if (ctx.cancelled) {
                    log.info("批阅已取消，批次 {} 跳过", index + 1);
                    return null;
                }
                ctx.pauseLock.lock();
                try {
                    while (ctx.paused && !ctx.cancelled) {
                        ctx.pausedCondition.await();
                    }
                } finally {
                    ctx.pauseLock.unlock();
                }
                if (ctx.cancelled) {
                    return null;
                }

                int from = index * 5;
                int to = Math.min(from + 5, totalStudents);
                List<StudentResult> batch = new ArrayList<>(students.subList(from, to));
                for (StudentResult s : batch) {
                    if (s.getStatus() != GradingTaskStatus.SUCCESS) {
                        s.setStatus(GradingTaskStatus.PROCESSING);
                    }
                }
                log.info("开始处理第 {}/{} 批 ({} 个学生)", index + 1, totalBatches, batch.size());
                callDifyForBatch(batch, ctx.rubric, ctx.taskId);
                // 更新计数（需保证 completedCount 是原子变量，若原为 int 请改为 AtomicInteger）
                for (StudentResult s : batch) {
                    if (s.getStatus() == GradingTaskStatus.SUCCESS) {
                        ctx.completedCount.incrementAndGet();
                    } else if (s.getStatus() != GradingTaskStatus.PENDING) {
                        ctx.failedCount.incrementAndGet();
                    }
                }
                saveProgress(batch, ctx.taskId);
                return null;
            });
        }

        try {
            List<Future<Void>> futures = virtualExecutor.invokeAll(batchTasks);
            for (Future<Void> f : futures) {
                f.get(); // 若有异常则抛出
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("批阅调度异常", e);
            updateGradingTaskStatus(ctx.taskId, GradingTaskStatus.FAILED);
            activeTasks.remove(ctx.taskId);
            return;
        }

        // 全部完成
        log.info("批阅全部完成: taskId={}, 已完成={}, 失败={}", ctx.taskId, ctx.completedCount, ctx.failedCount);
        if (ctx.cancelled) {
            updateGradingTaskStatus(ctx.taskId, GradingTaskStatus.CANCELLED);
        } else {
            updateGradingTaskStatus(ctx.taskId, GradingTaskStatus.SUCCESS);
        }
        activeTasks.remove(ctx.taskId);
    }

    /**
     * 一次性调用 Dify 批阅整批学生作业
     */
    private void callDifyForBatch(List<StudentResult> batch, List<RubricItem> rubric, String taskId) {
        List<StudentHomework> homeworks = new ArrayList<>();
        Map<String, StudentResult> studentMap = new LinkedHashMap<>();

        for (StudentResult s : batch) {
            if (GradingTaskStatus.SUCCESS == s.getStatus()) {
                log.info("跳过已批阅学生: {} ({})", s.getStudentName(), s.getStudentId());
                continue;
            }
            // 从 path 读取 docx 并转为 markdown
            String mdContent;
            try {
                CONVERT_SEM.acquire();   // 获取许可
                try {
                    mdContent = readWordContentAsMd(s.getWordContentPath());
                } finally {
                    CONVERT_SEM.release(); // 释放许可
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                s.setStatus(GradingTaskStatus.FAILED);
                s.setErrorMessage("转换被中断");
                continue;
            }
            if (mdContent == null) {
                s.setStatus(GradingTaskStatus.FAILED);
                s.setErrorMessage("无法读取或转换作业文件: " + s.getWordContentPath());
                continue;
            }
            StudentHomework hw = new StudentHomework(s.getStudentId(), s.getStudentName(), mdContent);
            hw.setPath(s.getWordContentPath());
            homeworks.add(hw);
            studentMap.put(s.getStudentId(), s);
        }

        if (homeworks.isEmpty()) {
            log.info("本批次所有学生已完成，跳过 Dify 调用");
            return;
        }

        log.info("调用 Dify 批阅 {} 个学生", homeworks.size());

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put(BusinessConstant.RUBRIC, new RubricRequest(rubric));
            requestBody.put(BusinessConstant.CONTENT, new ContentRequest(homeworks));
            requestBody.put(BusinessConstant.HANDLE_TYPE, BusinessConstant.COMMON);

            String response = difyClient.runWorkflowBlocking(difyProperty.getApiKey(), requestBody);
            log.info("Dify 响应: {}", response);
            String outputJson = extractOutputAsJson(response);
            if (outputJson == null) {
                for (StudentResult s : studentMap.values()) {
                    s.setStatus(GradingTaskStatus.FAILED);
                    s.setErrorMessage("Dify返回结果解析失败");
                }
                return;
            }

            JsonNode root = mapper.readTree(outputJson);
            JsonNode outputArray = root.get("output");
            if (outputArray == null || !outputArray.isArray()) {
                for (StudentResult s : studentMap.values()) {
                    s.setStatus(GradingTaskStatus.FAILED);
                    s.setErrorMessage("Dify输出格式异常: output 不是数组");
                }
                return;
            }

            List<StudentRagInsertDTO> ragInsertDTOs = new ArrayList<>();
            Set<String> respondedIds = new HashSet<>();
            for (JsonNode studentNode : outputArray) {
                String stuId = studentNode.path("stu_id").asText("");
                String stuName = studentNode.path("stu_name").asText("");
                String totalScore = studentNode.path("total_score").asText("");
                String contentJson = studentNode.path("content").asText("");

                if (stuId.isEmpty()) {
                    log.warn("跳过缺少学号的返回记录");
                    continue;
                }

                StudentResult sr = studentMap.get(stuId);
                if (sr == null) {
                    log.warn("Dify 返回的学号 {} 在当前批次中找不到对应学生", stuId);
                    continue;
                }

                String content = joinContent(contentJson);
                CommentJson commentJson = objectMapper.readValue(contentJson, CommentJson.class);

                StudentRagInsertDTO ragInsertDTO = StudentRagInsertDTO.builder()
                        .score(Float.parseFloat(totalScore))
                        .text(content)
                        .studentId(stuId)
                        .taskId(taskId)
                        .commentJson(commentJson)
                        .submitTime(System.currentTimeMillis())
                        .semester(stuId.substring(0,4))
                        .build();
                ragInsertDTOs.add(ragInsertDTO);

                sr.setStudentName(stuName.isEmpty() ? sr.getStudentName() : stuName);
                sr.setRawScore(totalScore);
                sr.setAiComment(content);
                sr.setCommentJson(commentJson);
                sr.setStatus(GradingTaskStatus.SUCCESS);
                sr.setErrorMessage(null);
                respondedIds.add(stuId);
                log.info("学生 {} 批阅完成: 得分={}", stuId, totalScore);
            }

            ragService.addContent(ragInsertDTOs);
            for (StudentResult s : studentMap.values()) {
                if (!respondedIds.contains(s.getStudentId())) {
                    s.setStatus(GradingTaskStatus.FAILED);
                    s.setErrorMessage("Dify 未返回该学生的批阅结果");
                    log.warn("学生 {} 未在 Dify 响应中找到", s.getStudentId());
                }
            }

        } catch (Exception e) {
            log.error("批次 Dify 调用失败: {}", e.getMessage(), e);
            for (StudentResult s : studentMap.values()) {
                s.setStatus(GradingTaskStatus.FAILED);
                s.setErrorMessage(e.getMessage());
            }
        }
    }


    @Override
    public void pauseReview(String taskId) {
        TaskContext ctx = activeTasks.get(taskId);
        if (ctx == null) {
            log.warn("任务不存在，无法暂停: taskId={}", taskId);
            return;
        }
        ctx.pauseLock.lock();
        try {
            ctx.paused = true;
            log.info("批阅已暂停: taskId={}", taskId);
        } finally {
            ctx.pauseLock.unlock();
        }
    }

    @Override
    public void resumeReview(String taskId) {
        TaskContext ctx = activeTasks.get(taskId);
        if (ctx == null) {
            log.warn("任务不存在，无法继续: taskId={}", taskId);
            return;
        }
        ctx.pauseLock.lock();
        try {
            ctx.paused = false;
            ctx.pausedCondition.signalAll();
            log.info("批阅已继续: taskId={}", taskId);
        } finally {
            ctx.pauseLock.unlock();
        }
    }

    @Override
    public void cancelReview(String taskId) {
        TaskContext ctx = activeTasks.get(taskId);
        if (ctx == null) {
            log.warn("任务不存在，无法取消: taskId={}", taskId);
            return;
        }
        ctx.cancelled = true;
        // 唤醒暂停等待的线程
        ctx.pauseLock.lock();
        try {
            ctx.pausedCondition.signalAll();
        } finally {
            ctx.pauseLock.unlock();
        }
        log.info("批阅已取消: taskId={}", taskId);
    }

    @Override
    public boolean isRunning(String taskId) {
        return activeTasks.containsKey(taskId);
    }

    @Override
    public boolean isPaused(String taskId) {
        TaskContext ctx = activeTasks.get(taskId);
        return ctx != null && ctx.paused;
    }

    @Override
    public void retryReview(String taskId) {
        GradingTask task = gradingTaskMapper.selectById(taskId);
        if (task == null || task.getDeleted() == 1) {
            throw new BusinessException(ErrorCodeConstant.NOT_FOUND, ErrorMessageConstant.TASK_NOT_FOUND);
        }
        if (task.getFileType() == FileType.WORD) {
            runWorkflowWithCommonFiles(List.of(task.getFileId()), task.getRubricId(),
                    task.getClassId(), task.getTaskCategory(), task.getTaskSequence());
        } else if (task.getFileType() == FileType.ZIP) {
            startBatchReviewFromZip(task.getFileId(), task.getRubricId(),
                    task.getClassId(), task.getTaskCategory(), task.getTaskSequence());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (!activeTasks.isEmpty()) {
            log.info("应用关闭，取消所有运行中的批阅任务，共 {} 个", activeTasks.size());
            for (TaskContext ctx : activeTasks.values()) {
                ctx.cancelled = true;
                ctx.pauseLock.lock();
                try {
                    ctx.pausedCondition.signalAll();
                } finally {
                    ctx.pauseLock.unlock();
                }
                if (ctx.future != null) {
                    ctx.future.cancel(true);
                }
            }
            activeTasks.clear();
        }
    }


    private String joinContent(String contentJson) throws JsonProcessingException {
        JsonNode node = objectMapper.readTree(contentJson);
        if (node == null || !node.isObject()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<JsonNode> elements = node.elements();
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            sb.append(element.asText());
            sb.append("\n");
        }
        return sb.toString();
    }


    /**
     * 持久化批阅结果到数据库（合并策略：支持重试场景）
     */
    private void saveProgress(List<StudentResult> batchStudents, String taskId) {
        log.info("保存批阅进度: taskId={}, 当前批学生数={}", taskId, batchStudents.size());

        for (StudentResult batchStudent : batchStudents) {
            // 先查数据库中是否已有该学生记录
            List<StudentResult> existing = studentResultMapper.selectList(
                    Wrappers.<StudentResult>lambdaQuery()
                            .eq(StudentResult::getTaskId, taskId)
                            .eq(StudentResult::getStudentId, batchStudent.getStudentId())
            );

            if (!existing.isEmpty()) {
                StudentResult dbRecord = existing.get(0);
                dbRecord.setRawScore(batchStudent.getRawScore());
                dbRecord.setAiComment(batchStudent.getAiComment());
                dbRecord.setStatus(batchStudent.getStatus());
                dbRecord.setErrorMessage(batchStudent.getErrorMessage());
                dbRecord.setStudentName(batchStudent.getStudentName());
                studentResultMapper.updateById(dbRecord);
            } else {
                studentResultMapper.insert(batchStudent);
            }
        }

        log.info("批阅进度已保存: taskId={}", taskId);
    }

    private void updateGradingTaskStatus(String taskId, GradingTaskStatus status) {
        GradingTask task = gradingTaskMapper.selectById(taskId);
        if (task != null) {
            task.setStatus(status);
            if (GradingTaskStatus.SUCCESS == status || GradingTaskStatus.FAILED == status) {
                task.setFinishTime(LocalDateTime.now());
            }
            gradingTaskMapper.updateById(task);
        }
    }


    /**
     * 两层解压总压缩包，提取所有学生作业信息
     * <pre>
     * 总压缩包.zip                    ← 第1层 unzip
     *  ├── 学生A_学号.zip             ← 第2层 unzip
     *  │   ├── 文档.docx
     *  │   └── 其他文件
     *  ├── 学生B_学号.zip
     *  │   └── 文档.docx
     *  └── ...
     * </pre>
     */
    private List<StudentHomework> extractFromTotalZip(String taskId ,String uploadId) throws Exception {
        Path baseDir = Path.of(tempUnzipDir, UUID.randomUUID().toString());
        List<StudentHomework> results = new ArrayList<>();

        UploadFileDTO uploadFileDTO = uploadService.selectById(uploadId);
        String totalZipPath = uploadFileDTO.getRawStoragePath();
        uploadFileDTO.setUnzipStoragePath(baseDir.toString());
        uploadService.updateById(uploadFileDTO);

        Files.createDirectories(baseDir);

        // 第1层：解压总压缩包
        ZipUtil.unzip(totalZipPath, baseDir.toFile());
        log.info("总压缩包已解压到: {}", baseDir);

        // 找到学生 zip 所在的目录（可能在根目录或某个子文件夹中）
        File studentZipDir = findStudentZipDir(baseDir.toFile());
        if (studentZipDir == null) {
            log.warn("未找到任何学生压缩包: {}", totalZipPath);
            return results;
        }

        // 第2层：逐个解压学生 zip 并提取 docx
        File[] studentZips = studentZipDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
        if (studentZips == null || studentZips.length == 0) {
            log.warn("学生 zip 目录为空: {}", studentZipDir.getAbsolutePath());
            return results;
        }

        for (File studentZip : studentZips) {
            try {
                StudentHomework hw = extractSingleStudentHomework(studentZip, baseDir);
                if (hw != null) {
                    results.add(hw);
                }
            } catch (Exception e) {
                log.error("处理学生压缩包失败: {} - {}", studentZip.getName(), e.getMessage(), e);
            }
        }

        log.info("从总压缩包提取到 {} 个学生作业", results.size());
        return results;


    }

    /**
     * 查找学生 zip 所在目录。
     * 先检查当前目录是否直接包含 .zip 文件；
     * 若只有一个子文件夹且无 zip，则递归进入该子文件夹查找。
     */
    private File findStudentZipDir(File dir) {
        File[] zips = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".zip"));
        if (zips != null && zips.length > 0) {
            return dir;
        }
        // 可能有一层包装文件夹
        File[] subdirs = dir.listFiles(File::isDirectory);
        if (subdirs != null && subdirs.length == 1) {
            return findStudentZipDir(subdirs[0]);
        }
        return null;
    }

    /**
     * 解压单个学生 zip 并提取 docx 文件路径，构造 StudentHomework
     */
    private StudentHomework extractSingleStudentHomework(File studentZip, Path baseDir) throws Exception {
        // 从文件名解析学号、姓名
        String[] info = parseStudentInfo(studentZip.getName());
        if (info == null) {
            log.warn("无法识别学号姓名: {}", studentZip.getName());
            return null;
        }
        String studentId = info[0];
        String studentName = info[1];

        // 解压到以学生姓名命名的子目录
        File studentDir = new File(baseDir.toFile(), studentName + "_" + studentId);
        ZipUtil.unzip(studentZip.getAbsolutePath(), studentDir);

        // 查找 Word 文档
        File wordFile = ZipUtil.findWordFile(studentDir);
        if (wordFile == null) {
            log.warn("未找到 Word 文档: {}", studentZip.getName());
            return null;
        }

        StudentHomework hw = new StudentHomework(studentId, studentName, null);
        hw.setPath(wordFile.getAbsolutePath());
        log.debug("提取学生作业: {} {} -> {}", studentId, studentName, wordFile.getAbsolutePath());
        return hw;
    }

    /**
     * 从文件名解析学号和姓名
     * @return [studentId, studentName] 或 null
     */
    private String[] parseStudentInfo(String fileName) {
        String baseName = fileName;
        int dotIdx = baseName.lastIndexOf('.');
        if (dotIdx > 0) {
            baseName = baseName.substring(0, dotIdx);
        }
        Matcher m = STUDENT_INFO_PATTERN.matcher(baseName);
        if (m.find()) {
            return new String[]{m.group(1), m.group(3).trim()};
        }
        return null;
    }

    /**
     * 从学号中提取学期信息（前4位为年份）
     * @param studentId 学号
     * @return 学期，如 2024
     */
    private String extractSemester(String studentId) {
        if (studentId == null || studentId.length() < 4) {
            return "";
        }
        return studentId.substring(0, 4);
    }

    // ==================== 文件转换辅助 ====================

    /**
     * 从 docx 路径读取并转换为 Markdown 内容
     */
    private String readWordContentAsMd(String docxPath) {
        if (docxPath == null || docxPath.isEmpty()) {
            return null;
        }
        File docxFile = new File(docxPath);
        if (!docxFile.exists()) {
            log.error("作业文件不存在: {}", docxPath);
            return null;
        }
        try {
            Map<String, List<String>> result = fileFormatUtil.word2Md(List.of(docxPath));
            List<String> files = result.get("files");
            if (files != null && !files.isEmpty()) {
                return files.get(0);
            }
            return null;
        } catch (Exception e) {
            log.error("转换 Word 为 Markdown 失败: {} - {}", docxPath, e.getMessage());
            return null;
        }
    }

    public String extractOutputAsJson(String difyResponseJson) {
        try {
            JsonNode root = mapper.readTree(difyResponseJson);

            if (!root.has("task_id")) {
                log.error("JSON 中未找到 'task_id' 键");
                return null;
            }

            JsonNode dataNode = root.get("data");
            if (dataNode == null || !dataNode.has("status")) {
                log.error("JSON 中未找到 'status' 键");
                return null;
            }

            String status = dataNode.get("status").asText();
            if (!"succeeded".equals(status)) {
                log.error("Dify 工作流调用失败，状态为：{}", status);
                return null;
            }

            JsonNode outputsNode = dataNode.get("outputs");
            if (outputsNode == null || !outputsNode.has("output")) {
                log.error("JSON 中未找到 'output' 键");
                return null;
            }

            JsonNode outputNode = outputsNode.get("output");
            ObjectNode resultJson = mapper.createObjectNode();
            resultJson.set("output", outputNode);
            return mapper.writeValueAsString(resultJson);

        } catch (Exception e) {
            log.error("JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

}