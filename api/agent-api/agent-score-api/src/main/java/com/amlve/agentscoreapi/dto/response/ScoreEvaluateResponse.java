package com.amlve.agentscoreapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 作业批改响应（Dify 返回的出参）
 */
@Data
public class ScoreEvaluateResponse {

    /**
     * 得分（0 ~ fullScore）
     */
    private Integer score;

    /**
     * 是否正确：1=满分，0=未满分
     */
    @JsonProperty("is_correct")
    private Integer isCorrect;

    /**
     * 错误明细（结构化对象）
     */
    @JsonProperty("error_detail")
    private ErrorDetail errorDetail;

    /**
     * 关联知识点（逗号分隔）
     */
    @JsonProperty("knowledge_points")
    private String knowledgePoints;

    /**
     * 批改解析（Markdown）
     */
    private String analysis;

    /**
     * 补救建议（满分时为空字符串）
     */
    private String suggestion;

    /**
     * 错误明细容器
     */
    @Data
    public static class ErrorDetail {

        /**
         * 错误列表（满分时为空数组）
         */
        private List<ErrorItem> errors;
    }

    /**
     * 单条错误
     */
    @Data
    public static class ErrorItem {

        /**
         * 错误位置（如"withdraw方法"、"第5行"）
         */
        private String location;

        /**
         * 错误类型：SYNTAX_ERROR / LOGIC_ERROR / CONCEPT_ERROR / FORMAT_ERROR / MISSING / OTHER
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
