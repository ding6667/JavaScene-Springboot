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

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamHomeworkServiceImpl implements ExamHomeworkService {

    private final ExamHomeworkSubmitMapper submitMapper;
    private final ExamQuestionMapper questionMapper;
    private final ExamGradingTaskMapper gradingTaskMapper;
    private final ExamGradingService gradingService;


    @Override
    @Transactional
    public void submit(HomeworkSubmitRequest request) {
        Long userId = SecurityUtils.getUserId();
        Long homeworkId = request.getHomeworkId();

        List<Long> questionIds = request.getAnswers().stream()
                .map(HomeworkSubmitRequest.AnswerItem::getQuestionId)
                .distinct()
                .toList();
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
                submitMapper.updateById(existing);
            } else {
                ExamHomeworkSubmit submit = new ExamHomeworkSubmit();
                submit.setHomeworkId(homeworkId);
                submit.setUserId(userId);
                submit.setQuestionId(item.getQuestionId());
                submit.setAnswerContent(item.getAnswerContent());
                submit.setSubmitTime(now);
                submitMapper.insert(submit);
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
            gradingTaskMapper.insert(task);
        } else {
            task.setTotalScore(totalScore);
            task.setStudentScore(null);
            task.setScoreDetail(null);
            task.setErrorMessage(null);
            task.setStatus(ExamGradingTaskStatus.PENDING);
            gradingTaskMapper.updateById(task);
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                gradingService.gradeAsync(homeworkId, userId);
            }
        });
    }
}