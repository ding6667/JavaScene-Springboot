package com.amlve.study.service.impl;

import com.amlve.agentscoreapi.service.ScoreAgentClient;
import com.amlve.agentscoreapi.dto.request.ScoreEvaluateRequest;
import com.amlve.agentscoreapi.dto.response.ScoreEvaluateResponse;
import com.amlve.common.enums.JudgeModeEnum;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.examapi.dto.response.QuestionDetailResponse;
import com.amlve.examapi.service.ExamQuestionApi;
import com.amlve.study.dto.request.CodeRunRequest;
import com.amlve.study.judge.CodeJudgeResult;
import com.amlve.study.judge.TestCase;
import com.amlve.study.mapper.StudyCodeRunRecordMapper;
import com.amlve.study.service.CodeRunService;
import com.amlve.study.tableEnt.StudyCodeRunRecord;
import com.amlve.studyapi.dto.resquest.ProgrammingJudgeRequest;
import com.amlve.studyapi.dto.response.ProgrammingJudgeResponse;
import com.amlve.studyapi.service.StudyProgramming;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 编程题判题 API 实现（供 exam 调用，任务书 3.8）。
 *
 * <p>作业场景：exam 把编程题提交转给 study 判题。判题分流与刷题场景一致
 * （两边判题逻辑相同、触发场景不同、结果表不同）：
 * TEST_CASE / MIXED 跑用例判分，AI_GRADING 且无用例时转 agent-score 判分；
 * 判完只写 study 侧的 study_code_run_record，不写 exam 的表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyProgrammingApiImpl implements StudyProgramming {

    private final ExamQuestionApi examQuestionApi;
    private final CodeRunService codeRunService;
    private final ScoreAgentClient scoreAgentClient;
    private final StudyCodeRunRecordMapper codeRunRecordMapper;

    @Override
    public Result<ProgrammingJudgeResponse> judgeProgramming(ProgrammingJudgeRequest request) {
        if (request == null || request.getQuestionId() == null || request.getUserId() == null) {
            return Result.error("submitId / userId / questionId 不能为空");
        }
        QuestionDetailResponse question = getQuestion(request.getQuestionId());
        List<TestCase> cases = toTestCases(question.getTestCases());

        // AI_GRADING 且未配置用例：转主观题判分（agent-score），不产生运行记录
        if (cases.isEmpty()) {
            if (JudgeModeEnum.fromCode(question.getJudgeMode()) == JudgeModeEnum.AI_GRADING) {
                return Result.success(judgeByScoreAgent(request, question));
            }
            return Result.error("该编程题没有测试用例，无法判分: questionId=" + question.getId());
        }

        CodeRunRequest codeRunRequest = new CodeRunRequest();
        codeRunRequest.setCodeContent(request.getCodeContent());
        codeRunRequest.setChapterId(question.getChapterId());
        codeRunRequest.setQuestionId(question.getId());

        CodeJudgeResult judgeResult = codeRunService.judge(codeRunRequest, cases, request.getUserId());

        // 一次运行一条：作业判题也写 study 侧运行记录
        StudyCodeRunRecord runRecord = judgeResult.toRunRecord(
                request.getUserId(), question.getChapterId(), question.getId(), request.getCodeContent());
        codeRunRecordMapper.insert(runRecord);

        int fullScore = question.getScore() == null ? 0 : question.getScore();
        ProgrammingJudgeResponse response = new ProgrammingJudgeResponse();
        response.setScore(judgeResult.score(fullScore));
        response.setPassed(judgeResult.allPassed());
        response.setPassedCount(judgeResult.getPassedCount());
        response.setTotalCount(judgeResult.getTotalCount());
        response.setOutput(judgeResult.getOutput());
        response.setErrorMsg(judgeResult.getErrorMsg());
        response.setErrorReason(judgeResult.getErrorReason());
        response.setAnalysis(question.getAnalysis());
        response.setKnowledgePoints(judgeResult.getKnowledgePoints() == null
                ? null : String.join(",", judgeResult.getKnowledgePoints()));
        response.setRunRecordId(runRecord.getId());
        return Result.success(response);
    }

    private ProgrammingJudgeResponse judgeByScoreAgent(ProgrammingJudgeRequest request,
                                                       QuestionDetailResponse question) {
        int fullScore = question.getScore() == null ? 0 : question.getScore();
        Integer score = null;
        try {
            ScoreEvaluateRequest scoreRequest = new ScoreEvaluateRequest();
            scoreRequest.setStudentAnswer(request.getCodeContent());
            scoreRequest.setStandardAnswer(question.getAnswer());
            scoreRequest.setQuestionType(question.getType().toString());
            scoreRequest.setFullScore(String.valueOf(fullScore));
            scoreRequest.setQuestionContent(question.getContent());
            scoreRequest.setChapterId(question.getChapterId().toString());
            ScoreEvaluateResponse scoreResponse = scoreAgentClient.evaluate(scoreRequest);
            if (scoreResponse != null) {
                score = scoreResponse.getScore();
            }
        } catch (Exception e) {
            log.warn("作业编程题 AI 判分失败: questionId={}, error={}", question.getId(), e.getMessage());
        }
        ProgrammingJudgeResponse response = new ProgrammingJudgeResponse();
        response.setScore(score);
        response.setPassed(score != null && score >= fullScore);
        response.setAnalysis(question.getAnalysis());
        return response;
    }

    private List<TestCase> toTestCases(List<QuestionDetailResponse.TestCaseItem> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            return List.of();
        }
        return testCases.stream()
                .filter(Objects::nonNull)
                .map(item -> new TestCase(
                        item.getCaseIndex() == null ? 0 : item.getCaseIndex(),
                        item.getInput(),
                        item.getExpectedOutput(),
                        item.getTimeLimit(),
                        item.getIsSample(),
                        item.getCaseType()))
                .toList();
    }

    private QuestionDetailResponse getQuestion(Long questionId) {
        Result<QuestionDetailResponse> questionDetail;
        try {
            questionDetail = examQuestionApi.getQuestionDetail(questionId);
        } catch (Exception e) {
            log.warn("获取题目详情失败，题目ID：{}", questionId, e);
            throw new BusinessException("获取题目详情失败");
        }
        if (questionDetail == null || questionDetail.getData() == null) {
            throw new BusinessException("题目不存在: " + questionId);
        }
        return questionDetail.getData();
    }
}
