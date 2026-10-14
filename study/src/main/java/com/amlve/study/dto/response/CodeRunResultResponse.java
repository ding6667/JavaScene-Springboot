package com.amlve.study.dto.response;

import com.amlve.agentcodeapi.dto.response.CodeQualityDTO;
import com.amlve.agentcodeapi.dto.response.CommonMistakeDTO;
import com.amlve.agentcodeapi.dto.response.ComplexityDTO;
import com.amlve.agentcodeapi.dto.response.ImprovementDTO;
import com.amlve.agentcodeapi.dto.response.RelatedKnowledgeDTO;
import com.amlve.agentcodeapi.dto.response.SimilarQuestionDTO;
import com.amlve.agentcodeapi.dto.response.StepDTO;
import lombok.Data;

import java.util.List;

/**
 * 代码运行结果响应（后端 → 前端，任务书 2.4 完整版）
 *
 * <p>状态口径变更：{@code status} 由 1/2/3 数字升级为力扣式字符串
 * AC/WA/TLE/MLE/RE/CE/OLE/SV/SE（破坏性变更，前端需同步适配）。
 *
 * <p>AI 完整丰富字段（codeQuality、complexity、steps、commonMistakes、improvements、
 * relatedKnowledge、similarQuestions、encouragement、learningSuggestion）
 * 后端一律组装并落库，前端未展示不影响后端落库。
 */
@Data
public class CodeRunResultResponse {

    /**
     * 记录ID
     */
    private Long runId;

    /**
     * 运行状态码：AC/WA/TLE/MLE/RE/CE/OLE/SV/SE
     */
    private String status;

    /**
     * 运行状态中文描述
     */
    private String statusDesc;

    /**
     * 程序标准输出
     */
    private String output;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 错误行号（编译器报的单个）
     */
    private Integer errorLine;

    /**
     * 运行耗时（毫秒）
     */
    private Integer runDuration;

    /**
     * 内存占用（KB）
     */
    private Integer memoryUsed;

    /**
     * 判题结果：1=全通过，0=部分通过，-1=未判题
     */
    private Integer judgeResult;

    /**
     * 通过用例数
     */
    private Integer passedCount;

    /**
     * 总用例数
     */
    private Integer totalCount;

    /**
     * 得分（编程题按用例通过比例折算；未判题为 null）
     */
    private Integer score;

    /**
     * 测试用例详情（与运行记录详情共用 {@link TestResultVO.CaseVO} 一套结构）
     */
    private List<TestResultVO.CaseVO> testResults;

    // ========== AI 调试返回（完整丰富版） ==========

    /**
     * 错误行号列表（AI 分析，可能多个）
     */
    private List<String> errorLineList;

    /**
     * 错误原因（人话解释）
     */
    private String errorReason;

    /**
     * 优化建议
     */
    private String optimizeSuggest;

    /**
     * 标准解题思路
     */
    private String standardIdea;

    /**
     * 关联知识点列表
     */
    private List<String> knowledgePoints;

    /**
     * 代码质量评分
     */
    private CodeQualityDTO codeQuality;

    /**
     * 复杂度分析
     */
    private ComplexityDTO complexity;

    /**
     * 分步引导（结构化，前端渲染需同步改造）
     */
    private List<StepDTO> steps;

    /**
     * 常见错误对比
     */
    private List<CommonMistakeDTO> commonMistakes;

    /**
     * 改进建议列表
     */
    private List<ImprovementDTO> improvements;

    /**
     * 关联教材（章节 + 页码）
     */
    private List<RelatedKnowledgeDTO> relatedKnowledge;

    /**
     * 相似题推荐
     */
    private List<SimilarQuestionDTO> similarQuestions;

    /**
     * 鼓励性话语
     */
    private String encouragement;

    /**
     * 学习建议（后续学习方向）
     */
    private String learningSuggestion;
}
