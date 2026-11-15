package com.amlve.knowledgeapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 内容校验请求
 * <p>
 * 供六大智能体（备课、出题、批改等）与教师侧调用：把 AI 生成内容与该章教材原文比对，
 * 判断是否贴合教材。
 */
@Data
public class KnowledgeVerifyRequest {

    /**
     * 校验归属章节
     */
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    /**
     * 待校验的 AI 生成内容（教案 / 出题 / 批改解析等）
     */
    @NotBlank(message = "待校验内容不能为空")
    private String aiAnswer;
}
