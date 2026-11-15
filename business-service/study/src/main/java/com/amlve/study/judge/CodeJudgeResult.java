package com.amlve.study.judge;

import com.amlve.agentcodeapi.dto.response.CodeQualityDTO;
import com.amlve.agentcodeapi.dto.response.CommonMistakeDTO;
import com.amlve.agentcodeapi.dto.response.ComplexityDTO;
import com.amlve.agentcodeapi.dto.response.ImprovementDTO;
import com.amlve.agentcodeapi.dto.response.RelatedKnowledgeDTO;
import com.amlve.agentcodeapi.dto.response.SimilarQuestionDTO;
import com.amlve.agentcodeapi.dto.response.StepDTO;
import com.amlve.common.enums.JudgeResultEnum;
import com.amlve.common.enums.RunStatusEnum;
import com.amlve.study.dto.response.TestResultVO;
import com.amlve.study.tableEnt.StudyCodeRunRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.List;

/**
 * 编程题判题结果（跑测试用例）
 *
 * <p>状态口径（任务书 5.2 定稿）：
 * {@code judgeStatus} 为权威运行状态（AC/WA/TLE/MLE/RE/CE/OLE/SV/SE），
 * {@code legacyStatus()} 保留旧 status 列（1-6）的兼容映射。
 */
@Data
public class CodeJudgeResult {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 权威运行状态 */
    private RunStatusEnum judgeStatus;
    /** 判题结果：全部通过 / 部分通过 / 未判题 */
    private JudgeResultEnum judgeResult;
    /** 汇总输出：全部通过时取最后一个用例输出，否则取第一个失败用例的实际输出 */
    private String output;
    private String errorMsg;
    private Integer errorLine;
    private Integer runDuration;
    /** 内存占用（KB），运行器暂未采集子进程 RSS，固定 0 */
    private Integer memoryUsed = 0;

    private int totalCount;
    private int passedCount;
    private List<CaseResult> cases = List.of();
    /** 测试用例执行结果（落库 study_code_run_record.test_result） */
    private TestResultVO testResult;

    // ===== AI 调试字段（完整丰富版） =====
    private List<String> errorLineList;
    private String errorReason;
    private String optimizeSuggest;
    private String standardIdea;
    private List<String> knowledgePoints;
    private CodeQualityDTO codeQuality;
    private ComplexityDTO complexity;
    private List<StepDTO> steps;
    private List<CommonMistakeDTO> commonMistakes;
    private List<ImprovementDTO> improvements;
    private List<RelatedKnowledgeDTO> relatedKnowledge;
    private List<SimilarQuestionDTO> similarQuestions;
    private String encouragement;
    private String learningSuggestion;

    public boolean allPassed() {
        return totalCount > 0 && passedCount == totalCount;
    }

    /** 按比例给分；无用例或无满分时为 0 */
    public int score(int fullScore) {
        if (totalCount <= 0 || fullScore <= 0) {
            return 0;
        }
        return Math.round(fullScore * (float) passedCount / totalCount);
    }

    /**
     * 转运行记录实体（一次运行一条，含 AI 完整丰富字段与判题字段）。
     * 刷题提交与作业判题两条链路共用，不得各自拼装。
     */
    public StudyCodeRunRecord toRunRecord(Long userId, Long chapterId, Long questionId, String code) {
        StudyCodeRunRecord entity = new StudyCodeRunRecord();
        entity.setUserId(userId);
        entity.setChapterId(chapterId);
        entity.setQuestionId(questionId);
        entity.setCodeContent(code);
        entity.setJudgeStatus(judgeStatus);
        entity.setStatus(legacyStatus());
        entity.setJudgeResult(judgeResult);
        entity.setTestResult(testResult);
        entity.setPassedCount(passedCount);
        entity.setTotalCount(totalCount);
        entity.setRunDuration(runDuration);
        entity.setMemoryUsed(memoryUsed == null ? 0 : memoryUsed);
        entity.setRunResult(output);
        entity.setErrorMsg(errorMsg);
        entity.setErrorLine(errorLine);
        entity.setErrorLineList(errorLineList);
        entity.setErrorReason(errorReason);
        entity.setOptimizeSuggest(optimizeSuggest);
        entity.setStandardIdea(standardIdea);
        entity.setKnowledgePoints(toKnowledgePointsJson());
        entity.setCodeQuality(codeQuality);
        entity.setComplexity(complexity);
        entity.setSteps(steps);
        entity.setCommonMistakes(commonMistakes);
        entity.setImprovements(improvements);
        entity.setRelatedKnowledge(relatedKnowledge);
        entity.setSimilarQuestions(similarQuestions);
        entity.setEncouragement(encouragement);
        entity.setLearningSuggestion(learningSuggestion);
        return entity;
    }

    /** 知识点列表序列化为 JSON 数组字符串（knowledge_points 列）；空列表返回 null */
    private String toKnowledgePointsJson() {
        if (knowledgePoints == null || knowledgePoints.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(knowledgePoints);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 旧 status 列（study_code_run_record.status，兼容期）的映射：
     * 1=成功 2=编译错误 3=运行时错误(含超时/内存) 4=安全拦截 5=内部错误 6=答案错误
     */
    public Integer legacyStatus() {
        if (judgeStatus == null) {
            return null;
        }
        return switch (judgeStatus) {
            case ACCEPTED -> 1;
            case COMPILE_ERROR -> 2;
            case RUNTIME_ERROR, TIME_LIMIT_EXCEEDED, MEMORY_LIMIT_EXCEEDED, OUTPUT_LIMIT_EXCEEDED -> 3;
            case SECURITY_VIOLATION -> 4;
            case SYSTEM_ERROR -> 5;
            case WRONG_ANSWER -> 6;
        };
    }

    public record CaseResult(
            int caseIndex, boolean passed,
            String input, String expectedOutput, String actualOutput, String errorMsg,
            Boolean isSample, String caseType, RunStatusEnum status
    ) {}
}
