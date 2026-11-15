package com.amlve.study.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 代码运行记录详情（任务书 3.6）
 */
@Data
public class CodeRecordDetailVO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 章节ID
     */
    private Long chapterId;

    /**
     * 题目ID（null=自由练习）
     */
    private Long questionId;

    /**
     * 学生提交的代码
     */
    private String codeContent;

    /**
     * 运行输出结果
     */
    private String runResult;

    /**
     * 编译/运行时错误信息
     */
    private String errorMsg;

    /**
     * 错误行号（编译器报的单个）
     */
    private Integer errorLine;

    /**
     * 运行状态：AC/WA/TLE/MLE/RE/CE/OLE/SV/SE
     */
    private String status;

    /**
     * 运行状态中文描述
     */
    private String statusDesc;

    /**
     * 判题结果：1=全部通过，0=部分通过，null=未判题
     */
    private Integer judgeResult;

    /**
     * 测试用例执行结果
     */
    private TestResultVO testResult;

    /**
     * 通过用例数
     */
    private Integer passedCount;

    /**
     * 总用例数
     */
    private Integer totalCount;

    /**
     * 运行耗时（毫秒）
     */
    private Integer runDuration;

    /**
     * 内存占用（KB）
     */
    private Integer memoryUsed;

    // ===== AI 调试返回（完整丰富版） =====

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
     * 关联知识点
     */
    private List<String> knowledgePoints;

    /**
     * 代码质量评分
     */
    private com.amlve.agentcodeapi.dto.response.CodeQualityDTO codeQuality;

    /**
     * 复杂度分析
     */
    private com.amlve.agentcodeapi.dto.response.ComplexityDTO complexity;

    /**
     * 分步引导
     */
    private List<com.amlve.agentcodeapi.dto.response.StepDTO> steps;

    /**
     * 常见错误对比
     */
    private List<com.amlve.agentcodeapi.dto.response.CommonMistakeDTO> commonMistakes;

    /**
     * 改进建议列表
     */
    private List<com.amlve.agentcodeapi.dto.response.ImprovementDTO> improvements;

    /**
     * 关联教材
     */
    private List<com.amlve.agentcodeapi.dto.response.RelatedKnowledgeDTO> relatedKnowledge;

    /**
     * 相似题推荐
     */
    private List<com.amlve.agentcodeapi.dto.response.SimilarQuestionDTO> similarQuestions;

    /**
     * 鼓励性话语
     */
    private String encouragement;

    /**
     * 学习建议
     */
    private String learningSuggestion;

    /**
     * 运行时间
     */
    private LocalDateTime createTime;
}
