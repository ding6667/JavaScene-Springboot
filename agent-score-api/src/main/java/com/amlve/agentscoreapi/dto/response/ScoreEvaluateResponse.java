package com.amlve.agentscoreapi.dto.response;


import lombok.Data;
import java.util.List;

/**
 * Dify 批改响应 DTO
 */
@Data
public class ScoreEvaluateResponse {

    /**
     * 得分
     */
    private Integer score;

    /**
     * 错误明细列表
     */
    private List<ErrorDetail> errorDetails;

    /**
     * 总体评价
     */
    private String analysis;

    /**
     * 知识点补救建议
     */
    private String suggestion;

    /**
     * 是否完全正确（true=满分/正确，false=有失分）
     */
    private Boolean isCorrect;

    /**
     * 涉及知识点列表（用于错题归档与补救推荐）
     */
    private List<String> knowledgePoints;

    @Data
    public static class ErrorDetail {

        /**
         * 错误位置（行号/题号）
         */
        private String location;

        /**
         * 错误类型（语法错误、逻辑错误、概念混淆、格式错误）
         */
        private String errorType;

        /**
         * 错误描述
         */
        private String errorDescription;

        /**
         * 失分原因
         */
        private String reason;

        /**
         * 修改建议
         */
        private String suggestion;
    }
}