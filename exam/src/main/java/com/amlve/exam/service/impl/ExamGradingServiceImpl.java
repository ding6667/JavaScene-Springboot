package com.amlve.exam.service.impl;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.agentscoreapi.dto.request.ScoreEvaluateRequest;
import com.amlve.agentscoreapi.dto.response.ScoreEvaluateResponse;
import com.amlve.agentscoreapi.service.ScoreAgentClient;
import com.amlve.common.exception.BusinessException;
import com.amlve.exam.dto.response.GradingResultResponse;
import com.amlve.exam.enumeration.ExamGradingTaskStatus;
import com.amlve.exam.mapper.ExamGradingTaskMapper;
import com.amlve.exam.mapper.ExamHomeworkSubmitMapper;
import com.amlve.exam.mapper.ExamQuestionMapper;
import com.amlve.exam.service.ExamGradingService;
import com.amlve.exam.service.ExamWrongQuestionService;
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

/**
 * 答题批改服务实现。
 * <p>
 * 异步逐题调用评分智能体（{@link ScoreAgentClient}），汇总得分与错误原因写回批改任务。
 * 批改运行在异步线程，异常无法向外回传，故统一记录日志并把失败信息写入批改任务的 errorMessage，
 * 便于前端/排查时定位。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamGradingServiceImpl implements ExamGradingService {

    private final ExamGradingTaskMapper gradingTaskMapper;
    private final ExamHomeworkSubmitMapper submitMapper;
    private final ExamQuestionMapper questionMapper;
    private final ScoreAgentClient scoreClient;
    private final ExamWrongQuestionService wrongQuestionService;

    /**
     * 异步批改指定学生某次作业的全部提交。
     *
     * @param homeworkId 作业 ID
     * @param userId     学生 ID
     */
    @Async("examGradingExecutor")
    @Override
    public void gradeAsync(Long homeworkId, Long userId) {
        try {
            doGrade(homeworkId, userId);
        } catch (Exception e) {
            log.error("作业批改失败: homeworkId={}, userId={}", homeworkId, userId, e);
            markFailed(homeworkId, userId, e);
        }
    }

    /**
     * 执行批改主流程：逐题评分并写回批改任务结果。
     */
    private void doGrade(Long homeworkId, Long userId) {
        List<ExamHomeworkSubmit> submits = submitMapper.selectList(Wrappers.<ExamHomeworkSubmit>lambdaQuery()
                .eq(ExamHomeworkSubmit::getHomeworkId, homeworkId)
                .eq(ExamHomeworkSubmit::getUserId, userId));
        if (submits == null || submits.isEmpty()) {
            log.warn("作业无提交记录，跳过批改: homeworkId={}, userId={}", homeworkId, userId);
            return;
        }

        List<Long> questionIds = submits.stream()
                .map(ExamHomeworkSubmit::getQuestionId)
                .distinct()
                .toList();
        List<ExamQuestion> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, ExamQuestion> questionMap = (questions == null ? List.<ExamQuestion>of() : questions).stream()
                .collect(Collectors.toMap(ExamQuestion::getId, Function.identity()));

        int studentScore = 0;
        List<ExamGradingTask.ScoreDetailItem> details = new ArrayList<>();
        for (ExamHomeworkSubmit submit : submits) {
            ExamQuestion question = questionMap.get(submit.getQuestionId());
            if (question == null) {
                log.warn("提交的题目不存在，跳过: questionId={}", submit.getQuestionId());
                continue;
            }
            int fullScore = question.getScore() == null ? 0 : question.getScore();
            int score = 0;
            String reason;
            try {
                ScoreEvaluateRequest request = new ScoreEvaluateRequest();
                request.setStudentAnswer(submit.getAnswerContent());
                request.setStandardAnswer(question.getAnswer() == null ? "" : question.getAnswer());
                request.setQuestionType(question.getType() == null ? null : String.valueOf(question.getType().getCode()));
                request.setFullScore(String.valueOf(fullScore));
                request.setQuestionContent(question.getContent());
                request.setChapterId(question.getChapterId() == null ? null : String.valueOf(question.getChapterId()));
                request.setQuestionId(question.getId() == null ? null : String.valueOf(question.getId()));

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

            if (score < fullScore) {
                try {
                    wrongQuestionService.archive(userId, question.getId(), submit.getAnswerContent(), 1);
                } catch (Exception e) {
                    log.warn("错题归档失败: userId={}, questionId={}", userId, question.getId(), e);
                }
            }
        }

        ExamGradingTask task = gradingTaskMapper.selectOne(Wrappers.<ExamGradingTask>lambdaQuery()
                .eq(ExamGradingTask::getHomeworkId, homeworkId)
                .eq(ExamGradingTask::getUserId, userId));
        if (task == null) {
            log.warn("批改任务不存在，无法写回结果: homeworkId={}, userId={}", homeworkId, userId);
            return;
        }
        task.setStudentScore(studentScore);
        task.setScoreDetail(details);
        task.setStatus(ExamGradingTaskStatus.DONE);
        task.setErrorMessage(null);
        if (gradingTaskMapper.updateById(task) == 0) {
            log.warn("批改结果写回失败: homeworkId={}, userId={}", homeworkId, userId);
        }
    }

    /**
     * 批改异常时把失败信息写入批改任务，便于前端展示与排查。
     */
    private void markFailed(Long homeworkId, Long userId, Exception e) {
        ExamGradingTask task = gradingTaskMapper.selectOne(Wrappers.<ExamGradingTask>lambdaQuery()
                .eq(ExamGradingTask::getHomeworkId, homeworkId)
                .eq(ExamGradingTask::getUserId, userId));
        if (task == null) {
            return;
        }
        task.setErrorMessage("批改失败: " + e.getMessage());
        task.setStatus(ExamGradingTaskStatus.FAILED);
        gradingTaskMapper.updateById(task);
    }

    /**
     * 查询指定学生某次作业的批改结果。
     *
     * @param homeworkId 作业 ID
     * @param userId     学生 ID
     * @return 批改结果
     */
    @Override
    public GradingResultResponse getByHomeworkAndUser(Long homeworkId, Long userId) {
        ExamGradingTask task = gradingTaskMapper.selectOne(Wrappers.<ExamGradingTask>lambdaQuery()
                .eq(ExamGradingTask::getHomeworkId, homeworkId)
                .eq(ExamGradingTask::getUserId, userId));
        if (task == null) {
            // TODO 常量字段提取为静态变量，放入constant
            throw new BusinessException(ErrorMessageConstant.GRADING_RESULT_NOT_FOUND);
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

    /**
     * 将评分智能体返回的分析与错误明细拼接为可读的错误原因。
     */
    private String buildReason(ScoreEvaluateResponse response) {
        StringBuilder sb = new StringBuilder();
        if (response.getAnalysis() != null && !response.getAnalysis().isEmpty()) {
            sb.append(response.getAnalysis());
        }
        if (response.getErrorDetail() != null && response.getErrorDetail().getErrors() != null) {
            for (ScoreEvaluateResponse.ErrorItem item : response.getErrorDetail().getErrors()) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(item.getErrorType());
                if (item.getErrorDescription() != null && !item.getErrorDescription().isEmpty()) {
                    sb.append(": ").append(item.getErrorDescription());
                }
            }
        }
        return sb.toString();
    }
}
