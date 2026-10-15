package com.amlve.agentcodeapi.dto.response;

import lombok.Data;

import java.util.List;

/**
 * Dify 代码调试响应（完整丰富版）
 *
 * <p>包含：基础错误信息 + 代码质量评分 + 复杂度分析 + 分步引导 +
 * 常见错误对比 + 改进建议 + 关联教材 + 相似题推荐 + 鼓励话语 + 学习建议。
 *
 * <p>结构化子对象统一放在 {@code com.amlve.agentcodeapi.dto.response} 包下只留一份，
 * study 侧 {@code CodeRunResultResponse} 直接复用，不得重复定义。
 */
@Data
public class CodeDebugResponse {

    // ========== 基础错误信息 ==========

    /**
     * 错误行号列表
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
     * 关联知识点
     */
    private List<String> knowledgePoints;

    // ========== 代码质量评分 ==========

    /**
     * 代码质量评分
     */
    private CodeQualityDTO codeQuality;

    // ========== 复杂度分析 ==========

    /**
     * 复杂度分析
     */
    private ComplexityDTO complexity;

    // ========== 分步引导 ==========

    /**
     * 分步引导（学生自己思考的路径，不给答案）。
     * 由 List&lt;String&gt; 升级为 {@link StepDTO} 结构，前端渲染需同步改造。
     */
    private List<StepDTO> steps;

    // ========== 常见错误对比 ==========

    /**
     * 常见错误对比
     */
    private List<CommonMistakeDTO> commonMistakes;

    // ========== 代码改进建议 ==========

    /**
     * 改进建议列表
     */
    private List<ImprovementDTO> improvements;

    // ========== 关联教材 ==========

    /**
     * 关联教材章节 + 页码
     */
    private List<RelatedKnowledgeDTO> relatedKnowledge;

    // ========== 相似题推荐 ==========

    /**
     * 相似题推荐
     */
    private List<SimilarQuestionDTO> similarQuestions;

    // ========== 鼓励性话语 ==========

    /**
     * 鼓励性话语
     */
    private String encouragement;

    // ========== 学习建议 ==========

    /**
     * 学习建议（后续学习方向）
     */
    private String learningSuggestion;
}
