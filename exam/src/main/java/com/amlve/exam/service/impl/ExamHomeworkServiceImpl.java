package com.amlve.exam.service.impl;

import com.amlve.common.exception.BusinessException;
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
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 作业提交服务实现。
 * <p>
 * 学生按题提交答案，逐题写入提交记录并创建/重置批改任务；
 * 事务提交后触发异步批改。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamHomeworkServiceImpl implements ExamHomeworkService {

    private final ExamHomeworkSubmitMapper submitMapper;
    private final ExamQuestionMapper questionMapper;
    private final ExamGradingTaskMapper gradingTaskMapper;
    private final ExamGradingService gradingService;

    /**
     * 学生提交作业答案，并在事务提交后触发异步批改。
     *
     * @param request 作业提交请求
     * @throws BusinessException 题目不存在或落库失败时抛出
     */
    @Override
    @Transactional
    public void submit(HomeworkSubmitRequest request) {
        Long userId = SecurityUtils.getUserId();
        Long homeworkId = request.getHomeworkId();

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
                gradingService.gradeAsync(homeworkId, userId);
            }
        });
    }
}
