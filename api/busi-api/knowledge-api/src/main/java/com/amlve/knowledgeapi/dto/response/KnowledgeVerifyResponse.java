package com.amlve.knowledgeapi.dto.response;

import lombok.Data;

/**
 * AI 内容校验结果
 */
@Data
public class KnowledgeVerifyResponse {

    /**
     * 是否贴合教材（命中率 ≥ 阈值）
     */
    private Boolean isMatch;

    /**
     * 关键词命中率，保留两位小数（0.00 ~ 1.00）
     */
    private Double hitRate;

    /**
     * 命中的教材原文（取与命中关键词重合度最高的片段，截断 500 字符）
     */
    private String standardAnswer;

    /**
     * 未命中关键词提示与修改建议（贴合教材时为 null）
     */
    private String suggestRevision;
}
