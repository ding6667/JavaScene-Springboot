package com.amlve.study.service.impl;

import com.amlve.agentscoreapi.dto.request.ScoreEvaluateRequest;
import com.amlve.agentscoreapi.dto.response.ScoreEvaluateResponse;
import com.amlve.agentscoreapi.service.ScoreAgentClient;
import com.amlve.common.enums.JudgeModeEnum;
import com.amlve.common.enums.QuestionTypeEnum;
import com.amlve.common.enums.RunStatusEnum;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.examapi.dto.response.QuestionDetailResponse;
import com.amlve.examapi.service.ExamQuestionApi;
import com.amlve.study.AnswerValidator;
import com.amlve.study.dto.request.CodeRunRequest;
import com.amlve.study.dto.request.PracticeDraftRequest;
import com.amlve.study.dto.request.PracticeSubmitRequest;
import com.amlve.study.dto.response.PracticeDetailResponse;
import com.amlve.study.dto.response.PracticeDraftResponse;
import com.amlve.study.dto.response.PracticeSubmitResponse;
import com.amlve.study.judge.CodeJudgeResult;
import com.amlve.study.judge.JudgeResult;
import com.amlve.study.judge.JudgeService;
import com.amlve.study.judge.TestCase;
import com.amlve.study.mapper.PracticeMapper;
import com.amlve.study.mapper.QuestionStatusMapper;
import com.amlve.study.mapper.StudyCodeRunRecordMapper;
import com.amlve.study.service.CodeRunService;
import com.amlve.study.service.PracticeService;
import com.amlve.study.tableEnt.StudyCodeRunRecord;
import com.amlve.study.tableEnt.StudyPracticeRecord;
import com.amlve.study.tableEnt.StudyQuestionStatus;
import com.amlve.studyapi.constant.BehaviorType;
import com.amlve.studyapi.dto.request.StudyBehaviorReportRequest;
import com.amlve.studyapi.service.StudyBehaviorApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 学生自主刷题（任务书 3.2 ~ 3.4）。
 *
 * <p>幂等口径：practice 一题只有一条（uk_user_question，存在则更新、
 * attempt_count / duration 累加、run_record_id 改指最新一次运行），
 * run 一次运行一条；study_question_status 与 practice 在同一事务内更新。
 *
 * <p>判题分流（刷题全归 study 判，不经过 exam）：
 * <ul>
 *   <li>客观题（选择/填空/判断/程序阅读）：本地字符串比对</li>
 *   <li>编程题（TEST_CASE / MIXED）：跑测试用例判分</li>
 *   <li>编程题（AI_GRADING 且无用例）、简答题：调 agent-score 判分</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PracticeServiceImpl
        extends ServiceImpl<PracticeMapper, StudyPracticeRecord>
        implements PracticeService {
    private final ExamQuestionApi examQuestionApi;
    private final AnswerValidator answerValidator;
    private final JudgeService judgeService;
    private final CodeRunService codeRunService;
    private final PracticeRecordWriter recordWriter;
    private final ScoreAgentClient scoreAgentClient;
    private final QuestionStatusMapper questionStatusMapper;
    private final StudyCodeRunRecordMapper codeRunRecordMapper;
    private final StudyBehaviorApi behaviorApi;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public PracticeSubmitResponse submit(PracticeSubmitRequest request, Long currentUserId) {
        // exam-api 获取题目详情（含答案、测试用例、judge_mode）
        QuestionDetailResponse question = getQuestion(request.getQuestionId());
        // 按题型二次校验答案形态
        answerValidator.validate(question, request.getAnswerContent());

        QuestionTypeEnum type = QuestionTypeEnum.fromCode(question.getType());
        if (type != null && type.isObjective()) {
            return submitObjective(request, question, currentUserId);
        }
        if (type == QuestionTypeEnum.PROGRAMMING) {
            return submitProgramming(request, question, currentUserId);
        }
        if (type == QuestionTypeEnum.SHORT_ANSWER) {
            return submitSubjective(request, question, currentUserId);
        }
        throw new BusinessException("暂不支持的题型: " + question.getType());
    }

    // ===== 客观题 =====

    private PracticeSubmitResponse submitObjective(PracticeSubmitRequest request,
                                                   QuestionDetailResponse question,
                                                   Long currentUserId) {
        JudgeResult result = judgeService.judge(question, request.getAnswerContent());

        StudyPracticeRecord existing = findPractice(currentUserId, question.getId());
        StudyPracticeRecord record = buildSubmitRecord(existing, request, question, currentUserId);
        record.setIsCorrect(result.correct() ? 1 : 0);
        record.setScore(result.score());
        record.setRunRecordId(null);
        record.setStatus(1);
        record.setSubmitTime(LocalDateTime.now());

        StudyQuestionStatus questionStatus = buildQuestionStatus(
                currentUserId, question.getId(), record.getIsCorrect());
        Long practiceId = recordWriter.savePractice(record, questionStatus);

        reportSubmitBehavior(currentUserId, question, record, result.correct() ? 1 : 0, result.score());

        PracticeSubmitResponse response = new PracticeSubmitResponse();
        response.setPracticeId(practiceId);
        response.setRunId(null);
        response.setIsCorrect(result.correct() ? 1 : 0);
        response.setScore(result.score());
        return response;
    }

    // ===== 编程题：按 judge_mode 分流判题 =====

    private PracticeSubmitResponse submitProgramming(PracticeSubmitRequest request,
                                                     QuestionDetailResponse question,
                                                     Long currentUserId) {
        List<TestCase> cases = toTestCases(question.getTestCases());
        JudgeModeEnum mode = JudgeModeEnum.fromCode(question.getJudgeMode());

        // AI_GRADING 且未配置用例：转主观题判分（agent-score）
        if (cases.isEmpty()) {
            if (mode == JudgeModeEnum.AI_GRADING) {
                return submitSubjective(request, question, currentUserId);
            }
            throw new BusinessException("该编程题没有测试用例,无法判分 : questionId = " + question.getId());
        }

        CodeRunRequest codeRunRequest = new CodeRunRequest();
        codeRunRequest.setCodeContent(request.getAnswerContent());
        codeRunRequest.setChapterId(question.getChapterId());
        codeRunRequest.setQuestionId(question.getId());

        // 耗时动作：事务外执行
        CodeJudgeResult judgeResult = codeRunService.judge(codeRunRequest, cases, currentUserId);

        int fullScore = question.getScore() == null ? 0 : question.getScore();
        Integer score;
        Integer correct;
        if (judgeResult.getJudgeStatus() == RunStatusEnum.SYSTEM_ERROR) {
            // 判题失败：practice 仍落库为已提交，isCorrect / 得分置空并记 warn
            log.warn("编程题判题失败（系统错误）: userId={}, questionId={}, error={}",
                    currentUserId, question.getId(), judgeResult.getErrorMsg());
            score = null;
            correct = null;
        } else {
            score = judgeResult.score(fullScore);
            correct = judgeResult.allPassed() ? 1 : 0;
        }

        // 组装两条记录 + 题目状态，同一事务落库
        StudyCodeRunRecord runRecord = judgeResult.toRunRecord(
                currentUserId, question.getChapterId(), question.getId(), request.getAnswerContent());
        StudyPracticeRecord existing = findPractice(currentUserId, question.getId());
        StudyPracticeRecord practiceRecord = buildSubmitRecord(existing, request, question, currentUserId);
        practiceRecord.setIsCorrect(correct);
        practiceRecord.setScore(score);
        practiceRecord.setStatus(1);
        practiceRecord.setSubmitTime(LocalDateTime.now());

        StudyQuestionStatus questionStatus = buildQuestionStatus(currentUserId, question.getId(), correct);
        PracticeRecordWriter.SavedIds ids = recordWriter.saveProgramming(runRecord, practiceRecord, questionStatus);

        reportSubmitBehavior(currentUserId, question, practiceRecord, correct, score);

        PracticeSubmitResponse response = new PracticeSubmitResponse();
        response.setPracticeId(ids.practiceId());
        response.setRunId(ids.runId());
        response.setIsCorrect(correct);
        response.setScore(score);
        response.setPassedCount(judgeResult.getPassedCount());
        response.setTotalCount(judgeResult.getTotalCount());
        response.setCaseResults(judgeResult.getCases().stream()
                .map(c -> {
                    PracticeSubmitResponse.CaseResult item = new PracticeSubmitResponse.CaseResult();
                    item.setCaseIndex(c.caseIndex());
                    item.setPassed(c.passed() ? 1 : 0);
                    item.setInput(c.input());
                    item.setExpectedOutput(c.expectedOutput());
                    item.setActualOutput(c.actualOutput());
                    item.setErrorMsg(c.errorMsg());
                    return item;
                })
                .toList());
        response.setOutput(judgeResult.getOutput());
        response.setErrorMsg(judgeResult.getErrorMsg());
        response.setErrorLine(judgeResult.getErrorLine());
        response.setErrorReason(judgeResult.getErrorReason());
        response.setOptimizeSuggest(judgeResult.getOptimizeSuggest());
        response.setStandardIdea(judgeResult.getStandardIdea());
        response.setKnowledgePoints(judgeResult.getKnowledgePoints());
        return response;
    }

    // ===== 主观题（简答题 / AI_GRADING 编程题）：调 agent-score 判分 =====

    private PracticeSubmitResponse submitSubjective(PracticeSubmitRequest request,
                                                    QuestionDetailResponse question,
                                                    Long currentUserId) {
        int fullScore = question.getScore() == null ? 0 : question.getScore();
        Integer score = null;
        Integer correct = null;
        try {
            ScoreEvaluateRequest scoreRequest = new ScoreEvaluateRequest();
            scoreRequest.setStudentAnswer(request.getAnswerContent());
            scoreRequest.setStandardAnswer(question.getAnswer());
            scoreRequest.setQuestionType(question.getType());
            scoreRequest.setFullScore(fullScore);
            scoreRequest.setQuestionContent(question.getContent());
            scoreRequest.setChapterId(question.getChapterId());
            ScoreEvaluateResponse scoreResponse = scoreAgentClient.evaluate(scoreRequest);
            if (scoreResponse != null && scoreResponse.getScore() != null) {
                score = scoreResponse.getScore();
                correct = score >= fullScore ? 1 : 0;
            }
        } catch (Exception e) {
            // 判分失败：practice 仍落库为已提交，isCorrect / 得分置空并记 warn
            log.warn("主观题 AI 判分失败: userId={}, questionId={}, error={}",
                    currentUserId, question.getId(), e.getMessage());
        }

        StudyPracticeRecord existing = findPractice(currentUserId, question.getId());
        StudyPracticeRecord record = buildSubmitRecord(existing, request, question, currentUserId);
        record.setIsCorrect(correct);
        record.setScore(score);
        record.setRunRecordId(null);
        record.setStatus(1);
        record.setSubmitTime(LocalDateTime.now());

        StudyQuestionStatus questionStatus = buildQuestionStatus(currentUserId, question.getId(), correct);
        Long practiceId = recordWriter.savePractice(record, questionStatus);

        reportSubmitBehavior(currentUserId, question, record, correct, score);

        PracticeSubmitResponse response = new PracticeSubmitResponse();
        response.setPracticeId(practiceId);
        response.setRunId(null);
        response.setIsCorrect(correct);
        response.setScore(score);
        return response;
    }

    // ===== 草稿（任务书 3.4） =====

    @Override
    public PracticeDraftResponse draft(PracticeDraftRequest request, Long currentUserId) {
        StudyPracticeRecord existing = findPractice(currentUserId, request.getQuestionId());
        StudyPracticeRecord record = existing == null ? new StudyPracticeRecord() : existing;
        record.setUserId(currentUserId);
        record.setQuestionId(request.getQuestionId());
        // 草稿允许清空代码：answerContent 为 null 时不覆盖已有答案
        if (request.getAnswerContent() != null) {
            record.setAnswerContent(request.getAnswerContent());
        }
        if (request.getDuration() != null) {
            record.setDuration(request.getDuration());
        }
        if (request.getAttemptCount() != null) {
            record.setAttemptCount(request.getAttemptCount());
        }
        // 已提交（status=1）后仍允许改草稿，但不回退提交状态、不重算 isCorrect / 得分
        if (record.getStatus() == null) {
            record.setStatus(0);
        }

        Long practiceId = recordWriter.saveDraft(record);

        reportDraftBehavior(currentUserId, request);

        PracticeDraftResponse response = new PracticeDraftResponse();
        response.setPracticeId(practiceId);
        response.setSaveStatus(1);
        return response;
    }

    // ===== 详情（任务书 3.3） =====

    @Override
    public PracticeDetailResponse detail(Long currentUserId, Long questionId) {
        StudyPracticeRecord record = findPractice(currentUserId, questionId);
        if (record == null) {
            // 未作答：返回空结构而非 404，前端按「未作答」渲染
            PracticeDetailResponse empty = new PracticeDetailResponse();
            empty.setQuestionId(questionId);
            empty.setStatus(0);
            return empty;
        }

        PracticeDetailResponse response = new PracticeDetailResponse();
        response.setPracticeId(record.getId());
        response.setQuestionId(record.getQuestionId());
        response.setAnswerContent(record.getAnswerContent());
        response.setStatus(record.getStatus());
        response.setIsCorrect(record.getIsCorrect());
        response.setDuration(record.getDuration());
        response.setAttemptCount(record.getAttemptCount());
        response.setSubmitTime(record.getSubmitTime());
        response.setRunRecordId(record.getRunRecordId());
        response.setRunDetail(loadRunDetail(currentUserId, record.getRunRecordId()));
        return response;
    }

    /** 按 run_record_id 取最新一次运行结果摘要（只取最新，历史多次运行走记录列表查） */
    private PracticeDetailResponse.CodeRunDetail loadRunDetail(Long userId, Long runRecordId) {
        if (runRecordId == null) {
            return null;
        }
        StudyCodeRunRecord run = codeRunRecordMapper.selectById(runRecordId);
        if (run == null || !userId.equals(run.getUserId())) {
            return null;
        }
        PracticeDetailResponse.CodeRunDetail detail = new PracticeDetailResponse.CodeRunDetail();
        detail.setOutput(run.getRunResult());
        detail.setErrorMsg(run.getErrorMsg());
        detail.setErrorLine(run.getErrorLine());
        detail.setErrorReason(run.getErrorReason());
        detail.setOptimizeSuggest(run.getOptimizeSuggest());
        detail.setStandardIdea(run.getStandardIdea());
        detail.setKnowledgePoints(fromKnowledgePointsJson(run.getKnowledgePoints()));
        return detail;
    }

    // ===== 记录组装 =====

    /** 查本人本题的练习记录（uk_user_question，一题只有一条） */
    private StudyPracticeRecord findPractice(Long userId, Long questionId) {
        return getBaseMapper().selectOne(new LambdaQueryWrapper<StudyPracticeRecord>()
                .eq(StudyPracticeRecord::getUserId, userId)
                .eq(StudyPracticeRecord::getQuestionId, questionId)
                .last("LIMIT 1"));
    }

    /**
     * 提交场景的 upsert 载体：存在则更新（attempt_count / duration 累加），
     * 不存在则新增；isCorrect / score / runRecordId 由调用方按判题结果补充。
     */
    private StudyPracticeRecord buildSubmitRecord(StudyPracticeRecord existing,
                                                  PracticeSubmitRequest request,
                                                  QuestionDetailResponse question,
                                                  Long currentUserId) {
        StudyPracticeRecord record = existing == null ? new StudyPracticeRecord() : existing;
        record.setUserId(currentUserId);
        record.setQuestionId(question.getId());
        record.setChapterId(question.getChapterId());
        record.setPracticeMode(request.getPracticeMode() == null ? 1 : request.getPracticeMode());
        record.setAnswerContent(request.getAnswerContent());
        int baseAttempt = existing == null || existing.getAttemptCount() == null ? 0 : existing.getAttemptCount();
        int addAttempt = request.getAttemptCount() == null || request.getAttemptCount() < 1
                ? 1 : request.getAttemptCount();
        record.setAttemptCount(baseAttempt + addAttempt);
        int baseDuration = existing == null || existing.getDuration() == null ? 0 : existing.getDuration();
        record.setDuration(baseDuration + (request.getDuration() == null ? 0 : request.getDuration()));
        return record;
    }

    /** 题目状态 upsert 载体（与 practice 同一事务落库，刷题列表 done/correct 只读本表） */
    private StudyQuestionStatus buildQuestionStatus(Long userId, Long questionId, Integer isCorrect) {
        LocalDateTime now = LocalDateTime.now();
        StudyQuestionStatus existing = questionStatusMapper.selectOne(
                new LambdaQueryWrapper<StudyQuestionStatus>()
                        .eq(StudyQuestionStatus::getUserId, userId)
                        .eq(StudyQuestionStatus::getQuestionId, questionId)
                        .last("LIMIT 1"));
        if (existing == null) {
            StudyQuestionStatus status = new StudyQuestionStatus();
            status.setUserId(userId);
            status.setQuestionId(questionId);
            status.setAttemptCount(1);
            status.setLastSubmitTime(now);
            if (isCorrect != null && isCorrect == 1) {
                status.setStatus(2);
                status.setFirstAcTime(now);
            } else {
                status.setStatus(1);
            }
            return status;
        }
        existing.setAttemptCount((existing.getAttemptCount() == null ? 0 : existing.getAttemptCount()) + 1);
        if (isCorrect != null && isCorrect == 1) {
            existing.setStatus(2);
            if (existing.getFirstAcTime() == null) {
                existing.setFirstAcTime(now);
            }
        } else if (existing.getStatus() == null || existing.getStatus() < 1) {
            existing.setStatus(1);
        }
        existing.setLastSubmitTime(now);
        return existing;
    }

    /** 编程题运行记录由 {@link CodeJudgeResult#toRunRecord} 统一组装（与作业判题共用） */

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

    // ===== 行为埋点 =====

    private void reportSubmitBehavior(Long userId, QuestionDetailResponse question,
                                      StudyPracticeRecord record, Integer isCorrect, Integer score) {
        reportBehavior(BehaviorType.PRACTICE_SUBMIT, userId, question.getId(),
                question.getChapterId(), record == null ? null : record.getDuration(),
                isCorrect, score);
    }

    private void reportDraftBehavior(Long userId, PracticeDraftRequest request) {
        reportBehavior(BehaviorType.PRACTICE_DRAFT, userId, request.getQuestionId(),
                null, request.getDuration(), null, null);
    }

    private void reportBehavior(BehaviorType type, Long userId, Long questionId, Long chapterId,
                                Integer duration, Integer isCorrect, Integer score) {
        if (behaviorApi == null) {
            return;
        }
        try {
            StudyBehaviorReportRequest report = new StudyBehaviorReportRequest();
            report.setUserId(userId);
            report.setBehaviorType(type);
            report.setQuestionId(questionId);
            report.setChapterId(chapterId);
            report.setDuration(duration);
            Map<String, Object> detail = new LinkedHashMap<>();
            if (isCorrect != null) {
                detail.put("isCorrect", isCorrect);
            }
            if (score != null) {
                detail.put("score", score);
            }
            report.setDetail(detail);
            behaviorApi.report(report);
        } catch (Exception e) {
            log.warn("{} 行为上报失败（不影响主流程）: {}", type.getCode(), e.getMessage());
        }
    }

    // ===== 工具 =====

    private List<String> fromKnowledgePointsJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("知识点 JSON 解析失败: {}", json);
            return null;
        }
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
