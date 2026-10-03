package com.amlve.knowledgeapi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeVerifyResponse {
    private Boolean isMatch;          // 是否贴合教材
    private Double hitRate;           // 关键词命中率，保留两位小数
    private String standardAnswer;    // 命中的教材原文（取命中率最高片段）
    private String suggestRevision;   // 未命中关键词提示与修改建议
}
