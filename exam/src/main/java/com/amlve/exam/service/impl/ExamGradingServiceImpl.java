package com.amlve.exam.service.impl;

import com.amlve.agentscoreapi.ScoreAgentClient;
import com.amlve.agentscoreapi.dto.request.ScoreEvaluateRequest;
import com.amlve.agentscoreapi.dto.response.ScoreEvaluateResponse;
import com.amlve.common.exception.BusinessException;
import com.amlve.exam.dto.response.GradingResultResponse;
import com.amlve.exam.enumeration.ExamGradingTaskStatus;
import com.amlve.exam.mapper.ExamGradingTaskMapper;
import com.amlve.exam.mapper.ExamHomeworkSubmitMapper;
import com.amlve.exam.mapper.ExamQuestionMapper;
import com.amlve.exam.service.ExamGradingService;
import com.amlve.exam.tableEnt.ExamGradingTask;
import com.amlve.exam.tableEnt.ExamHomeworkSubmit;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamGradingServiceImpl implements ExamGradingService {

    private final ExamGradingTaskMapper gradingTaskMapper;
    private final ExamHomeworkSubmitMapper submitMapper;
    private final ExamQuestionMapper questionMapper;
    private final ScoreAgentClient scoreClient;

    @Async("examGradingExecutor")
    @Override
    public void gradeAsync(Long homeworkId, Long userId) {
        List<ExamHomeworkSubmit> submits = submitMapper.selectList(Wrappers.<ExamHomeworkSubmit>lambdaQuery()
                .eq(ExamHomeworkSubmit::getHomeworkId, homeworkId)
                .eq(ExamHomeworkSubmit::getUserId, userId));
        if (submits.isEmpty()) {
            return;
        }

        List<Long> questionIds = submits.stream()
                .map(ExamHomeworkSubmit::getQuestionId)
                .distinct()
                .toList();
        Map<Long, ExamQuestion> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(ExamQuestion::getId, Function.identity()));

        int studentScore = 0;
        List<ExamGradingTask.ScoreDetailItem> details = new ArrayList<>();
        for (ExamHomeworkSubmit submit : submits) {
            ExamQuestion question = questionMap.get(submit.getQuestionId());
            if (question == null) {
                continue;
            }
            int fullScore = question.getScore() == null ? 0 : question.getScore();
            int score = 0;
            String reason;
            try {
                ScoreEvaluateRequest request = new ScoreEvaluateRequest();
                request.setStudentAnswer(submit.getAnswerContent());
                request.setStandardAnswer(question.getAnswer() == null ? "" : question.getAnswer().getAnswer());
                request.setQuestionType(question.getType() == null ? null : question.getType().getCode());
                request.setFullScore(fullScore);
                request.setQuestionContent(question.getContent());
                request.setChapterId(question.getChapterId());

                ScoreEvaluateResponse response = scoreClient.evaluate(request);
                score = response.getScore() == null ? 0 : response.getScore();
                reason = buildReason(response);
            } catch (Exception e) {
                log.error("作业 {} 题目 {} 批改失败", homeworkId, submit.getQuestionId(), e);
                reason = "批改失败: " + e.getMessage();
            }
            score = Math.max(0, Math.min(score, fullScore));
            studentScore += score;

            ExamGradingTask.ScoreDetailItem item = new ExamGradingTask.ScoreDetailItem();
            item.setQuestionId(question.getId());
            item.setQuestionContent(question.getContent());
            item.setFullScore(fullScore);
            item.setScore(score);
            item.setErrorReason(reason);
            details.add(item);
        }

        ExamGradingTask task = gradingTaskMapper.selectOne(Wrappers.<ExamGradingTask>lambdaQuery()
                .eq(ExamGradingTask::getHomeworkId, homeworkId)
                .eq(ExamGradingTask::getUserId, userId));
        if (task != null) {
            task.setStudentScore(studentScore);
            task.setScoreDetail(details);
            task.setStatus(ExamGradingTaskStatus.DONE);
            task.setErrorMessage(null);
            gradingTaskMapper.updateById(task);
        }
    }

    @Override
    public GradingResultResponse getByHomeworkAndUser(Long homeworkId, Long userId) {
        ExamGradingTask task = gradingTaskMapper.selectOne(Wrappers.<ExamGradingTask>lambdaQuery()
                .eq(ExamGradingTask::getHomeworkId, homeworkId)
                .eq(ExamGradingTask::getUserId, userId));
        if (task == null) {
            throw new BusinessException("批改结果不存在");
        }
        GradingResultResponse response = new GradingResultResponse();
        response.setId(task.getId());
        response.setHomeworkId(task.getHomeworkId());
        response.setUserId(task.getUserId());
        response.setTotalScore(task.getTotalScore());
        response.setStudentScore(task.getStudentScore());
        response.setScoreDetail(task.getScoreDetail());
        response.setStatus(task.getStatus());
        response.setErrorMessage(task.getErrorMessage());
        return response;
    }

    private String buildReason(ScoreEvaluateResponse response) {
        StringBuilder sb = new StringBuilder();
        if (response.getAnalysis() != null && !response.getAnalysis().isEmpty()) {
            sb.append(response.getAnalysis());
        }
        if (response.getErrorDetails() != null) {
            for (ScoreEvaluateResponse.ErrorDetail detail : response.getErrorDetails()) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(detail.getErrorType());
                if (detail.getErrorDescription() != null && !detail.getErrorDescription().isEmpty()) {
                    sb.append(": ").append(detail.getErrorDescription());
                }
            }
        }
        return sb.toString();
    }
}