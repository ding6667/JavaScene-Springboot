package com.amlve.exam.service.impl;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.exam.dto.request.HomeworkSubmitRequest;
import com.amlve.exam.enumeration.ExamGradingTaskStatus;
import com.amlve.exam.mapper.ExamGradingTaskMapper;
import com.amlve.exam.mapper.ExamHomeworkSubmitMapper;
import com.amlve.exam.mapper.ExamQuestionMapper;
import com.amlve.exam.service.ExamGradingService;
import com.amlve.exam.service.ExamHomeworkService;
import com.amlve.exam.tableEnt.ExamGradingTask;
import com.amlve.exam.tableEnt.ExamHomeworkSubmit;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.examapi.dto.request.StudentHomeworkListQueryRequest;
import com.amlve.examapi.dto.response.StudentHomeworkItemVO;
import com.amlve.systemapi.dto.api.UserBaseDTO;
import com.amlve.systemapi.service.UserApi;
import com.amlve.teachapi.dto.request.StudentHomeworkQueryRequest;
import com.amlve.teachapi.dto.response.HomeworkSubmitCheckResponse;
import com.amlve.teachapi.dto.response.StudentHomeworkBriefResponse;
import com.amlve.teachapi.dto.response.StudentHomeworkDetailResponse;
import com.amlve.teachapi.service.TeachHomeworkService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 作业提交服务实现。
 * <p>
 * 学生按题提交答案，逐题写入提交记录并创建/重置批改任务；
 * 提交前经 teach-api 校验作业状态与截止时间，事务提交后回调 teach 并触发异步批改。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamHomeworkServiceImpl implements ExamHomeworkService {

    private final ExamHomeworkSubmitMapper submitMapper;
    private final ExamQuestionMapper questionMapper;
    private final ExamGradingTaskMapper gradingTaskMapper;
    private final ExamGradingService gradingService;
    private final ObjectProvider<TeachHomeworkService> teachHomeworkServiceProvider;
    private final ObjectProvider<UserApi> userApiProvider;

    /** 作业状态：2=已发布（学生可见范围之一） */
    private static final int HOMEWORK_STATUS_PUBLISHED = 2;

    /**
     * 学生提交作业答案，并在事务提交后触发异步批改。
     *
     * @param request 作业提交请求
     * @throws BusinessException 作业不可提交、题目不存在或落库失败时抛出
     */
    @Override
    @Transactional
    public void submit(HomeworkSubmitRequest request) {
        Long userId = SecurityUtils.getUserId();
        Long homeworkId = request.getHomeworkId();

        checkHomeworkSubmittable(homeworkId);

        List<Long> questionIds = request.getAnswers().stream()
                .map(HomeworkSubmitRequest.AnswerItem::getQuestionId)
                .distinct()
                .toList();
        if (questionIds.isEmpty()) {
            throw new BusinessException(ErrorMessageConstant.ANSWER_LIST_MISSING);
        }
        Map<Long, ExamQuestion> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(ExamQuestion::getId, Function.identity()));
        // 题目必须属于该作业：否则可拿别的作业/任意题库ID提交，污染总分与批改结果；
        // 顺带拿到每题对应的作业快照（快照ID + 发布时的分值），批改时用它把错题钉在发布那一版上
        Map<Long, HomeworkSnapshotRef> homeworkSnapshots = resolveHomeworkSnapshots(homeworkId, questionIds);

        int totalScore = 0;
        LocalDateTime now = LocalDateTime.now();
        for (HomeworkSubmitRequest.AnswerItem item : request.getAnswers()) {
            ExamQuestion question = questionMap.get(item.getQuestionId());
            if (question == null) {
                log.warn("QUESTION_NOT_FOUND 定位: {}", item.getQuestionId());
                throw new BusinessException(ErrorMessageConstant.QUESTION_NOT_FOUND);
            }
            HomeworkSnapshotRef ref = homeworkSnapshots.get(item.getQuestionId());
            // 分值口径以发布时固化的为准（教师可逐题改分），取不到才用题库当前分值
            totalScore += ref != null && ref.score() != null
                    ? ref.score()
                    : (question.getScore() == null ? 0 : question.getScore());
            // 作业快照ID：优先用 teach 回查到的，其次用前端回传的，都没有就记 0（按题库版本归档）
            Long teachSnapshotId = ref == null ? null : ref.snapshotId();
            if (teachSnapshotId == null) {
                teachSnapshotId = item.getSnapshotId();
            }
            Long snapshotId = teachSnapshotId == null ? 0L : teachSnapshotId;

            ExamHomeworkSubmit existing = submitMapper.selectOne(Wrappers.<ExamHomeworkSubmit>lambdaQuery()
                    .eq(ExamHomeworkSubmit::getHomeworkId, homeworkId)
                    .eq(ExamHomeworkSubmit::getUserId, userId)
                    .eq(ExamHomeworkSubmit::getQuestionId, item.getQuestionId()));
            if (existing != null) {
                existing.setAnswerContent(item.getAnswerContent());
                existing.setSnapshotId(snapshotId);
                existing.setSubmitTime(now);
                if (submitMapper.updateById(existing) == 0) {
                    log.warn("HOMEWORK_SUBMIT_SAVE_FAILED 定位: {}", item.getQuestionId());
                    throw new BusinessException(ErrorMessageConstant.HOMEWORK_SUBMIT_SAVE_FAILED);
                }
            } else {
                ExamHomeworkSubmit submit = new ExamHomeworkSubmit();
                submit.setHomeworkId(homeworkId);
                submit.setUserId(userId);
                submit.setQuestionId(item.getQuestionId());
                submit.setSnapshotId(snapshotId);
                submit.setAnswerContent(item.getAnswerContent());
                submit.setSubmitTime(now);
                if (submitMapper.insert(submit) == 0) {
                    log.warn("HOMEWORK_SUBMIT_SAVE_FAILED 定位: {}", item.getQuestionId());
                    throw new BusinessException(ErrorMessageConstant.HOMEWORK_SUBMIT_SAVE_FAILED);
                }
            }
        }

        ExamGradingTask task = gradingTaskMapper.selectOne(Wrappers.<ExamGradingTask>lambdaQuery()
                .eq(ExamGradingTask::getHomeworkId, homeworkId)
                .eq(ExamGradingTask::getUserId, userId));
        if (task == null) {
            task = new ExamGradingTask();
            task.setHomeworkId(homeworkId);
            task.setUserId(userId);
            task.setTotalScore(totalScore);
            task.setStatus(ExamGradingTaskStatus.PENDING);
            if (gradingTaskMapper.insert(task) == 0) {
                throw new BusinessException(ErrorMessageConstant.GRADING_TASK_CREATE_FAILED);
            }
        } else {
            task.setTotalScore(totalScore);
            task.setStudentScore(null);
            task.setScoreDetail(null);
            task.setErrorMessage(null);
            task.setStatus(ExamGradingTaskStatus.PENDING);
            if (gradingTaskMapper.updateById(task) == 0) {
                throw new BusinessException(ErrorMessageConstant.GRADING_TASK_RESET_FAILED);
            }
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notifyTeachSubmit(homeworkId);
                gradingService.gradeAsync(homeworkId, userId);
            }
        });
    }

    /**
     * 当前学生的作业列表：按学生档案的班级/年级向 teach 取作业，再补上本人的提交进度与批改结果。
     *
     * @param request 查询条件
     * @return 作业分页；学生无班级档案、teach 未就绪时返回空分页而非报错
     */
    @Override
    public PageResult<StudentHomeworkItemVO> listMyHomework(StudentHomeworkListQueryRequest request) {
        Long userId = SecurityUtils.getUserId();
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();

        // 1. 解析学生档案：班级 + 年级
        UserApi userApi = userApiProvider.getIfAvailable();
        if (userApi == null) {
            log.warn("system 模块未就绪，无法解析学生班级，返回空作业列表: userId={}", userId);
            return PageResult.empty(pageNum, pageSize);
        }
        Result<UserBaseDTO> userResult = userApi.getUserById(userId);
        UserBaseDTO user = userResult == null || userResult.getCode() != ErrorCodeConstant.SUCCESS
                ? null : userResult.getData();
        if (user == null || (user.getClassId() == null && !StringUtils.hasText(user.getGrade()))) {
            log.info("学生无班级/年级档案，返回空作业列表: userId={}", userId);
            return PageResult.empty(pageNum, pageSize);
        }

        // 2. 取作业（teach 侧已做班级 + 全年级 + 状态过滤）
        TeachHomeworkService teachHomeworkService = teachHomeworkServiceProvider.getIfAvailable();
        if (teachHomeworkService == null) {
            log.warn("teach 模块未就绪，返回空作业列表: userId={}", userId);
            return PageResult.empty(pageNum, pageSize);
        }
        StudentHomeworkQueryRequest query = new StudentHomeworkQueryRequest();
        query.setClassId(user.getClassId());
        query.setGrade(user.getGrade());
        query.setStatus(request.getStatus());
        query.setChapterId(request.getChapterId());
        query.setOnlyNotExpired(request.getOnlyNotExpired());
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);

        Result<PageResult<StudentHomeworkBriefResponse>> pageResult = teachHomeworkService.pageStudentHomework(query);
        if (pageResult == null || pageResult.getCode() != ErrorCodeConstant.SUCCESS || pageResult.getData() == null) {
            log.warn("查询学生作业列表失败: userId={}, message={}", userId,
                    pageResult == null ? "响应为空" : pageResult.getMessage());
            return PageResult.empty(pageNum, pageSize);
        }
        PageResult<StudentHomeworkBriefResponse> page = pageResult.getData();
        // 命中 0 条时常见原因是作业发布范围与学生的班级/年级不一致，打全上下文便于定位
        log.info("学生作业列表查询: userId={}, classId={}, grade={}, status={}, total={}",
                userId, user.getClassId(), user.getGrade(), request.getStatus(), page.getTotal());
        List<StudentHomeworkBriefResponse> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), List.of());
        }

        // 3. 批量取本人在这些作业下的提交记录与批改任务，避免逐条查询
        List<Long> homeworkIds = records.stream()
                .map(StudentHomeworkBriefResponse::getHomeworkId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, List<ExamHomeworkSubmit>> submitsByHomework = new HashMap<>();
        Map<Long, ExamGradingTask> taskByHomework = new HashMap<>();
        if (!homeworkIds.isEmpty()) {
            submitMapper.selectList(Wrappers.<ExamHomeworkSubmit>lambdaQuery()
                            .eq(ExamHomeworkSubmit::getUserId, userId)
                            .in(ExamHomeworkSubmit::getHomeworkId, homeworkIds))
                    .forEach(submit -> submitsByHomework
                            .computeIfAbsent(submit.getHomeworkId(), key -> new ArrayList<>())
                            .add(submit));
            gradingTaskMapper.selectList(Wrappers.<ExamGradingTask>lambdaQuery()
                            .eq(ExamGradingTask::getUserId, userId)
                            .in(ExamGradingTask::getHomeworkId, homeworkIds))
                    .forEach(task -> taskByHomework.putIfAbsent(task.getHomeworkId(), task));
        }

        // 4. 组装视图
        LocalDateTime now = LocalDateTime.now();
        List<StudentHomeworkItemVO> list = records.stream()
                .map(brief -> toStudentItem(brief, submitsByHomework.get(brief.getHomeworkId()),
                        taskByHomework.get(brief.getHomeworkId()), now))
                .toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), list);
    }

    /**
     * 作业简要信息 + 本人提交进度 → 我的作业列表项。
     */
    private StudentHomeworkItemVO toStudentItem(StudentHomeworkBriefResponse brief,
                                                List<ExamHomeworkSubmit> submits,
                                                ExamGradingTask task,
                                                LocalDateTime now) {
        StudentHomeworkItemVO vo = new StudentHomeworkItemVO();
        vo.setHomeworkId(brief.getHomeworkId());
        vo.setTitle(brief.getTitle());
        vo.setContent(brief.getContent());
        vo.setChapterId(brief.getChapterId());
        vo.setChapterName(brief.getChapterName());
        vo.setQuestionCount(brief.getQuestionCount());
        vo.setTotalScore(brief.getTotalScore());
        vo.setDeadline(brief.getDeadline());
        vo.setAllowLateSubmit(brief.getAllowLateSubmit());
        vo.setPublishTime(brief.getPublishTime());
        vo.setStatus(brief.getStatus());
        vo.setStatusName(brief.getStatusName());

        List<ExamHomeworkSubmit> rows = submits == null ? List.of() : submits;
        vo.setSubmitted(!rows.isEmpty());
        vo.setSubmittedQuestionCount(rows.size());
        vo.setLastSubmitTime(rows.stream()
                .map(ExamHomeworkSubmit::getSubmitTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null));

        if (task != null && task.getStatus() != null) {
            vo.setGradingStatus(task.getStatus().getValue());
            vo.setGradingStatusName(task.getStatus().getDesc());
            // 只有批改完成才有有效得分，失败/待批改都给 null 让前端展示「待批改」
            if (task.getStatus() == ExamGradingTaskStatus.DONE) {
                vo.setStudentScore(task.getStudentScore());
            }
        } else {
            vo.setGradingStatusName("");
        }

        boolean expired = brief.getDeadline() != null && now.isAfter(brief.getDeadline());
        boolean published = Integer.valueOf(HOMEWORK_STATUS_PUBLISHED).equals(brief.getStatus());
        vo.setExpired(expired);
        vo.setCanSubmit(published && (!expired || Integer.valueOf(1).equals(brief.getAllowLateSubmit())));
        return vo;
    }

    /**
     * 提交前经 teach-api 校验作业状态、截止时间与是否允许补交。
     * <p>
     * teach 实现不在容器中（如单独启动 exam 模块）时跳过校验并打印告警，避免阻断本地开发。
     *
     * @param homeworkId 作业ID
     * @throws BusinessException 作业不可提交时抛出
     */
    private void checkHomeworkSubmittable(Long homeworkId) {
        TeachHomeworkService teachHomeworkService = teachHomeworkServiceProvider.getIfAvailable();
        if (teachHomeworkService == null) {
            log.warn("teach 模块未就绪，跳过作业提交校验: homeworkId={}", homeworkId);
            return;
        }
        Result<HomeworkSubmitCheckResponse> checkResult = teachHomeworkService.checkHomeworkSubmit(homeworkId);
        if (checkResult == null || checkResult.getCode() != ErrorCodeConstant.SUCCESS) {
            throw new BusinessException(checkResult == null ? ErrorMessageConstant.HOMEWORK_CHECK_FAILED : checkResult.getMessage());
        }
        HomeworkSubmitCheckResponse check = checkResult.getData();
        if (check != null && Boolean.FALSE.equals(check.getSubmittable())) {
            throw new BusinessException(StringUtils.hasText(check.getReason()) ? check.getReason() : ErrorMessageConstant.HOMEWORK_NOT_SUBMITTABLE);
        }
    }

    /**
     * 校验「提交的题目确实属于该作业」，并顺带取出每道题对应的作业快照ID。
     * <p>
     * 快照里的 {@code questionId} 为题库ID，提交入参也按题库ID走，两边可直接比对；
     * {@code id} 是 teach_homework_question.id，批改时用它把错题钉在发布那一版题上。
     * teach 不可用、作业详情取不到、或老快照整批缺题库ID时跳过校验并打 warn，
     * 不因为依赖缺失把正常提交挡死。
     *
     * @param homeworkId  作业ID
     * @param questionIds 本次提交的题库题目ID
     * @return 题库题目ID → 作业快照ID；依赖不可用时返回空 Map
     */
    private Map<Long, HomeworkSnapshotRef> resolveHomeworkSnapshots(Long homeworkId, List<Long> questionIds) {
        Map<Long, HomeworkSnapshotRef> snapshots = new HashMap<>();
        TeachHomeworkService teachHomeworkService = teachHomeworkServiceProvider.getIfAvailable();
        if (teachHomeworkService == null) {
            return snapshots;
        }
        Result<StudentHomeworkDetailResponse> detailResult = teachHomeworkService.getStudentHomeworkDetail(homeworkId);
        if (detailResult == null || detailResult.getCode() != ErrorCodeConstant.SUCCESS
                || detailResult.getData() == null || detailResult.getData().getQuestions() == null
                || detailResult.getData().getQuestions().isEmpty()) {
            log.warn("作业题目快照不可用，跳过题目归属校验: homeworkId={}", homeworkId);
            return snapshots;
        }
        Set<Long> allowed = new HashSet<>();
        for (StudentHomeworkDetailResponse.StudentQuestionItem item : detailResult.getData().getQuestions()) {
            if (item.getQuestionId() == null) {
                continue;
            }
            allowed.add(item.getQuestionId());
            snapshots.put(item.getQuestionId(), new HomeworkSnapshotRef(item.getId(), item.getScore()));
        }
        if (allowed.isEmpty()) {
            log.warn("作业快照缺少题库题目ID，跳过题目归属校验: homeworkId={}", homeworkId);
            return snapshots;
        }
        List<Long> illegal = questionIds.stream().filter(id -> !allowed.contains(id)).toList();
        if (!illegal.isEmpty()) {
            log.warn("提交题目不属于该作业: homeworkId={}, illegalQuestionIds={}", homeworkId, illegal);
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "提交的题目不属于该作业: " + illegal);
        }
        return snapshots;
    }

    /**
     * 作业题目在「发布态」下的两个关键值：快照ID（错题钉版本用）与分值（总分口径用）。
     * 分值取 teach 快照的 score（教师可逐题改分，为空时 teach 已用题库默认分兜底）。
     */
    private record HomeworkSnapshotRef(Long snapshotId, Integer score) {
    }

    /**
     * 提交成功后回调 teach 更新提交人数；失败只记录日志，不影响批改流程。
     *
     * @param homeworkId 作业ID
     */
    private void notifyTeachSubmit(Long homeworkId) {
        TeachHomeworkService teachHomeworkService = teachHomeworkServiceProvider.getIfAvailable();
        if (teachHomeworkService == null) {
            return;
        }
        try {
            teachHomeworkService.notifySubmit(homeworkId);
        } catch (Exception e) {
            log.warn("通知 teach 提交成功失败: homeworkId={}", homeworkId, e);
        }
    }
}