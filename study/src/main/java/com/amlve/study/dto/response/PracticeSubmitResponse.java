package com.amlve.study.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class PracticeSubmitResponse {

    /**
     * 练习记录ID
     */
    private Long practiceId;

    /**
     * 代码运行记录ID（非代码题可为 null）
     */
    private Long runId;

    /**
     * 是否正确
     */
    private Integer isCorrect;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 运行输出（代码题）
     */
    private String output;

    /**
     * 错误信息（代码题）
     */
    private String errorMsg;

    /**
     * 错误行号
     */
    private Integer errorLine;

    /**
     * AI 错误原因（代码题）
     */
    private String errorReason;

    /**
     * AI 优化建议
     */
    private String optimizeSuggest;

    /**
     * AI 标准解题思路
     */
    private String standardIdea;

    /**
     * 关联知识点
     */
    private List<String> knowledgePoints;

    /**
     * 通过用例数（代码题）
     */
    private Integer passedCount;

    /**
     * 用例总数（代码题）
     */
    private Integer totalCount;

    /**
     * 用例明细（代码题，前端可展示「第 N 个用例没过」）
     */
    private List<CaseResult> caseResults;

    @Data
    public static class CaseResult {
        private Integer caseIndex;
        private Integer passed;
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private String errorMsg;
    }
}