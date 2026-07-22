package com.amlve.ai.service.impl;

import com.amlve.ai.bean.DifyClient;
import com.amlve.ai.service.GradingService;
import com.amlve.ai.constant.BusinessConstant;
import com.amlve.ai.constant.ErrorCodeConstant;
import com.amlve.ai.constant.ErrorMessageConstant;
import com.amlve.ai.entity.StudentHomework;
import com.amlve.ai.tableEnt.GradingTask;
import com.amlve.ai.enumeration.GradingTaskStatus;
import com.amlve.common.dto.StandardConfigDTO;
import com.amlve.common.dto.UploadFileDTO;
import com.amlve.common.entity.RubricItem;
import com.amlve.common.exception.BusinessException;
import com.amlve.ai.properties.DifyProperty;
import com.amlve.common.service.StandardService;
import com.amlve.common.service.UploadService;
import com.amlve.ai.tableEnt.StudentResult;
import com.amlve.common.utils.FileFormatUtil;
import com.amlve.common.utils.ZipUtil;
import com.amlve.ai.mapper.GradingTaskMapper;
import com.amlve.ai.mapper.StudentResultMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradingServiceImpl implements GradingService {

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

    @Qualifier("gradingExecutor")
    private final ThreadPoolTaskExecutor gradingExecutor;
    @Qualifier("coordinatorExecutor")
    private final ThreadPoolTaskExecutor coordinatorExecutor;

    private final ObjectMapper mapper = new ObjectMapper();

    // 匹配学号和姓名
    private static final Pattern STUDENT_INFO_PATTERN = Pattern.compile("^(\\d{10})(\\s*)([\\u4e00-\\u9fa5·]{2,16})");

    private final Map<String, TaskContext> activeTasks = new ConcurrentHashMap<>();

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
        int completedCount;
        int failedCount;
        Future<?> future;
        TaskContext(String taskId) {
            this.taskId = taskId;
        }
    }



    /**
     * 异步
     * @param uploadIds 上传的文件ID列表
     * @param standardConfigId 评分标准ID
     */
    @Override
    @Async("coordinatorExecutor")
    public void runWorkflowWithCommonFiles(List<String> uploadIds, String standardConfigId) {
        if (uploadIds.isEmpty()) {
            throw new BusinessException(ErrorMessageConstant.FILES_NOT_FOUND);
        }
        Map<String,StudentHomework> studentHomeworks = initRecordsAndReturnMdFiles(uploadIds,standardConfigId);
        List<RubricItem> rubric = standardConfigService.selectById(standardConfigId).getStandardContent();

        List<CompletableFuture<Void>>  futures = studentHomeworks.entrySet().stream()
                        .map(entry ->{
                            String taskId = entry.getKey();
                            StudentHomework studentHomework = entry.getValue();
                            return CompletableFuture.runAsync(() -> processSingleFile(taskId,studentHomework,rubric),gradingExecutor);
                                }).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void processSingleFile(String taskId,StudentHomework studentHomework, List<RubricItem> rubric) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(BusinessConstant.RUBRIC, rubric);
        requestBody.put(BusinessConstant.CONTENT, List.of(studentHomework));
        requestBody.put(BusinessConstant.HANDLE_TYPE, BusinessConstant.COMMON);

        GradingTask gradingTask = gradingTaskMapper.selectById(taskId);
        try {
            gradingTask.setStatus(GradingTaskStatus.PROCESSING);
            gradingTask.setProcessedStudentCount(1);
            gradingTaskMapper.updateById(gradingTask);

            String result = difyClient.runWorkflowBlocking(difyProperty.getApiKey(), requestBody);
            String outputJson = extractOutputAsJson(result);

            if (outputJson == null) {
                throw new BusinessException(ErrorCodeConstant.SYSTEM_ERROR,ErrorMessageConstant.SYSTEM_ERROR);
            }
            JsonNode root = mapper.readTree(outputJson);
            JsonNode outputArray = root.get("output");
            if (outputArray == null || !outputArray.isArray()) {
                throw new BusinessException(ErrorCodeConstant.SYSTEM_ERROR,ErrorMessageConstant.SYSTEM_ERROR);
            }

            String totalScore = "";
            String review = "";

            for (JsonNode studentNode : outputArray) {
                totalScore = studentNode.path("total_score").asText("");
                review = studentNode.path("review").asText("");
            }
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
            updateGradingTaskStatus(taskId,GradingTaskStatus.SUCCESS);
        } catch (Exception e) {
            gradingTask.setStatus(GradingTaskStatus.FAILED);
            gradingTask.setErrorMessage(ErrorMessageConstant.GRADING_TASK_EXCEPTION_TERMINATED);
            gradingTaskMapper.updateById(gradingTask);
            throw new BusinessException(ErrorCodeConstant.UNKNOWN_ERROR,ErrorMessageConstant.GRADING_TASK_EXCEPTION_TERMINATED);
        }
    }

    /**
     * 初始化任务记录并返回学生作业对象
     * @param uploadIds 上传的文件ID列表
     * @param standardConfigId 评分标准ID
     * @return 任务ID到学生作业对象的映射
     */
    private Map<String,StudentHomework> initRecordsAndReturnMdFiles(List<String> uploadIds,String standardConfigId){
        Map<String,StudentHomework> mdFiles = new HashMap<>();
        uploadIds.forEach(uploadId -> {
            String taskId = UUID.randomUUID().toString();
            GradingTask task = new GradingTask();
            task.setId(taskId);
            task.setFileId(uploadId);
            task.setRubricId(standardConfigId);
            task.setStatus(GradingTaskStatus.PENDING);
            task.setCreateTime(LocalDateTime.now());
            task.setProcessedStudentCount(0);
            gradingTaskMapper.insert(task);
            UploadFileDTO uploadFile = uploadService.selectById(uploadId);
            String rawStoragePath = uploadFile.getRawStoragePath();
            Map<String, List<String>> mdMap = fileFormatUtil.word2Md(List.of(rawStoragePath));
            String content = mdMap.get(BusinessConstant.FILES).get(0);
            String[] studentInfoParts = parseStudentInfo(uploadFile.getFileName());
            if (studentInfoParts == null || studentInfoParts.length != 2 ) {
                throw new BusinessException(ErrorMessageConstant.FILE_NAME_INVALID);
            }
            mdFiles.put(taskId,new StudentHomework(studentInfoParts[0],studentInfoParts[1],content));
        });
        return mdFiles;
    }
    /**
     * 启动批阅
     * HTTP 线程只做三件事：生成 taskId、插入任务记录（PENDING）、提交到线程池，然后立刻返回。
     * 解压、创建学生记录、批阅全部在线程池中异步执行。
     */
    @Override
    public String startBatchReviewFromZip(String uploadId, String standardConfigId) {
        String taskId = UUID.randomUUID().toString();

        GradingTask task = new GradingTask();
        task.setId(taskId);
        task.setFileId(uploadId);
        task.setRubricId(standardConfigId);
        task.setStatus(GradingTaskStatus.PENDING);
        task.setCreateTime(LocalDateTime.now());
        task.setProcessedStudentCount(0);
        gradingTaskMapper.insert(task);

        TaskContext ctx = new TaskContext(taskId);

        ctx.future = gradingExecutor.submit(() -> {
            try {
                // 获取评分标准
                List<RubricItem> rubric = null;
                if (standardConfigId != null && !standardConfigId.isEmpty()) {
                    StandardConfigDTO config = standardConfigService.selectById(standardConfigId);
                    if (config != null) {
                        rubric = config.getStandardContent();
                    }
                }

                // 解压总 zip，提取学生作业
                String rawStoragePath = uploadService.selectById(uploadId).getRawStoragePath();
                List<StudentHomework> homeworks = extractFromTotalZip(rawStoragePath);

                if (homeworks.isEmpty()) {
                    throw new BusinessException(ErrorCodeConstant.FILE_NOT_FOUND, ErrorMessageConstant.FILES_NOT_FOUND);
                }

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

    /**
     * 批阅调度线程主逻辑：循环处理批次，每批 5 个学生一次性调用 Dify
     */
    private void runBatchReviewScheduler(TaskContext ctx) {
        int totalStudents = ctx.students.size();
        int totalBatches = (totalStudents + 4) / 5;
        log.info("开始批阅: taskId={}, 总学生数={}, 总批次数={}", ctx.taskId, totalStudents, totalBatches);

        try {
            for (int batchIdx = 0; batchIdx < totalBatches; batchIdx++) {
                // 暂停检查点
                ctx.pauseLock.lock();
                try {
                    while (ctx.paused && !ctx.cancelled) {
                        log.info("批阅已暂停，等待继续... (taskId={}, 当前批次 {}/{})", ctx.taskId, batchIdx, totalBatches);
                        ctx.pausedCondition.await();
                    }
                } finally {
                    ctx.pauseLock.unlock();
                }

                if (ctx.cancelled) {
                    log.info("批阅已被取消，停止调度 (taskId={}, 已完成 {} 批)", ctx.taskId, batchIdx);
                    break;
                }
                // 检查任务状态是否为处理中
                GradingTask gradingTask = gradingTaskMapper.selectById(ctx.taskId);
                if (gradingTask.getStatus() != GradingTaskStatus.PROCESSING){
                    log.info("批阅异常终端，停止调度 (taskId={}, 已完成 {} 批)", ctx.taskId, batchIdx);
                    throw new BusinessException(ErrorCodeConstant.UNKNOWN_ERROR,ErrorMessageConstant.GRADING_TASK_EXCEPTION_TERMINATED);
                }
                int from = batchIdx * 5;
                int to = Math.min(from + 5, totalStudents);
                List<StudentResult> batch = new ArrayList<>(ctx.students.subList(from, to));

                for (StudentResult s : batch) {
                    if (GradingTaskStatus.SUCCESS != s.getStatus()) {
                        s.setStatus(GradingTaskStatus.PROCESSING);
                    }
                }

                log.info("开始处理第 {}/{} 批 ({} 个学生, taskId={})", batchIdx + 1, totalBatches, batch.size(), ctx.taskId);

                callDifyForBatch(batch, ctx.rubric);

                for (StudentResult s : batch) {
                    if (GradingTaskStatus.SUCCESS == s.getStatus()) {
                        ctx.completedCount++;
                    } else if (GradingTaskStatus.PENDING != s.getStatus()) {
                        ctx.failedCount++;
                    }
                }

                log.info("第 {}/{} 批完成 (taskId={}, 累计已完成: {}, 失败: {})", batchIdx + 1, totalBatches, ctx.taskId, ctx.completedCount, ctx.failedCount);
                saveProgress(batch, ctx.taskId);

            }

            // 全部完成
            log.info("批阅全部完成: taskId={}, 已完成={}, 失败={}", ctx.taskId, ctx.completedCount, ctx.failedCount);
            if (ctx.cancelled) {
                updateGradingTaskStatus(ctx.taskId, GradingTaskStatus.CANCELLED);
            } else {
                updateGradingTaskStatus(ctx.taskId, GradingTaskStatus.SUCCESS);
            }


        } catch (InterruptedException e) {
            log.warn("批阅调度线程被中断，保存已批阅结果 (taskId={})", ctx.taskId);
            Thread.currentThread().interrupt();
            saveProgress(ctx.students, ctx.taskId);
            updateGradingTaskStatus(ctx.taskId, GradingTaskStatus.FAILED);

        } catch (Exception e) {
            log.error("批阅调度异常: taskId={}, {}", ctx.taskId, e.getMessage(), e);
            saveProgress(ctx.students, ctx.taskId);
            updateGradingTaskStatus(ctx.taskId, GradingTaskStatus.FAILED);

        } finally {
            activeTasks.remove(ctx.taskId);
        }
    }

    // ==================== Dify 调用 ====================

    /**
     * 一次性调用 Dify 批阅整批学生作业
     */
    private void callDifyForBatch(List<StudentResult> batch, List<RubricItem> rubric) {
        List<StudentHomework> homeworks = new ArrayList<>();
        Map<String, StudentResult> studentMap = new LinkedHashMap<>();

        for (StudentResult s : batch) {
            if (GradingTaskStatus.SUCCESS == s.getStatus()) {
                log.info("跳过已批阅学生: {} ({})", s.getStudentName(), s.getStudentId());
                continue;
            }
            // 从 path 读取 docx 并转为 markdown
            String mdContent = readWordContentAsMd(s.getWordContentPath());
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
            requestBody.put(BusinessConstant.RUBRIC, rubric);
            requestBody.put(BusinessConstant.CONTENT, homeworks);
            requestBody.put(BusinessConstant.HANDLE_TYPE, BusinessConstant.COMMON);

            String response = difyClient.runWorkflowBlocking(difyProperty.getApiKey(), requestBody);

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


            Set<String> respondedIds = new HashSet<>();
            for (JsonNode studentNode : outputArray) {
                String stuId = studentNode.path("stu_id").asText("");
                String stuName = studentNode.path("stu_name").asText("");
                String totalScore = studentNode.path("total_score").asText("");
                String review = studentNode.path("review").asText("");

                if (stuId.isEmpty()) {
                    log.warn("跳过缺少学号的返回记录");
                    continue;
                }

                StudentResult sr = studentMap.get(stuId);
                if (sr == null) {
                    log.warn("Dify 返回的学号 {} 在当前批次中找不到对应学生", stuId);
                    continue;
                }

                sr.setStudentName(stuName.isEmpty() ? sr.getStudentName() : stuName);
                sr.setRawScore(totalScore);
                sr.setAiComment(review);
                sr.setStatus(GradingTaskStatus.SUCCESS);
                sr.setErrorMessage(null);
                respondedIds.add(stuId);
                log.info("学生 {} 批阅完成: 得分={}", stuId, totalScore);
            }

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


    /**
     * TODO 强制重启任务
     * 扫描当前task是否有对应的数据库记录
     */

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
    private List<StudentHomework> extractFromTotalZip(String totalZipPath) throws Exception {
        Path baseDir = Path.of(tempUnzipDir, UUID.randomUUID().toString());
        List<StudentHomework> results = new ArrayList<>();


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
