package com.amlve.knowledgeapi.dto.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeVerifyRequest {

    private Long chapterId;       // 校验归属章节
    private String aiAnswer;      // 待校验的 AI 生成内容（教案 / 出题 / 批改解析等）
}
