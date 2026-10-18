package com.amlve.exam.service.impl;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
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
import com.amlve.teachapi.dto.response.HomeworkSubmitCheckResponse;
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
import java.util.List;
import java.util.Map;
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
            throw new BusinessException("答案列表不能为空");
        }
        Map<Long, ExamQuestion> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(ExamQuestion::getId, Function.identity()));

        int totalScore = 0;
        LocalDateTime now = LocalDateTime.now();
        for (HomeworkSubmitRequest.AnswerItem item : request.getAnswers()) {
            ExamQuestion question = questionMap.get(item.getQuestionId());
            if (question == null) {
                throw new BusinessException("题目不存在: " + item.getQuestionId());
            }
            totalScore += question.getScore() == null ? 0 : question.getScore();

            ExamHomeworkSubmit existing = submitMapper.selectOne(Wrappers.<ExamHomeworkSubmit>lambdaQuery()
                    .eq(ExamHomeworkSubmit::getHomeworkId, homeworkId)
                    .eq(ExamHomeworkSubmit::getUserId, userId)
                    .eq(ExamHomeworkSubmit::getQuestionId, item.getQuestionId()));
            if (existing != null) {
                existing.setAnswerContent(item.getAnswerContent());
                existing.setSubmitTime(now);
                if (submitMapper.updateById(existing) == 0) {
                    throw new BusinessException("更新提交记录失败: questionId=" + item.getQuestionId());
                }
            } else {
                ExamHomeworkSubmit submit = new ExamHomeworkSubmit();
                submit.setHomeworkId(homeworkId);
                submit.setUserId(userId);
                submit.setQuestionId(item.getQuestionId());
                submit.setAnswerContent(item.getAnswerContent());
                submit.setSubmitTime(now);
                if (submitMapper.insert(submit) == 0) {
                    throw new BusinessException("保存提交记录失败: questionId=" + item.getQuestionId());
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
                throw new BusinessException("创建批改任务失败");
            }
        } else {
            task.setTotalScore(totalScore);
            task.setStudentScore(null);
            task.setScoreDetail(null);
            task.setErrorMessage(null);
            task.setStatus(ExamGradingTaskStatus.PENDING);
            if (gradingTaskMapper.updateById(task) == 0) {
                throw new BusinessException("重置批改任务失败");
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
            throw new BusinessException(checkResult == null ? "作业校验失败" : checkResult.getMessage());
        }
        HomeworkSubmitCheckResponse check = checkResult.getData();
        if (check != null && Boolean.FALSE.equals(check.getSubmittable())) {
            throw new BusinessException(StringUtils.hasText(check.getReason()) ? check.getReason() : "作业当前不可提交");
        }
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