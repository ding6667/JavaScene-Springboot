package com.amlve.agentcodeapi.dto.response;

import lombok.Data;

/**
 * 相似题推荐
 */
@Data
public class SimilarQuestionDTO {

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 题目内容摘要
     */
    private String content;

    /**
     * 相似度说明
     */
    private String reason;
}
