package com.amlve.knowledge.controller;

import com.amlve.common.request.Result;
import com.amlve.knowledgeapi.dto.request.KnowledgeVerifyRequest;
import com.amlve.knowledgeapi.dto.response.KnowledgeVerifyResponse;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 内容校验接口
 * <p>
 * 教材入库前的内容把关属教师侧运维能力，故限 TEACHER / ADMIN；
 * 智能体侧（备课、出题、批改）走 KnowledgeChapterApi 服务内调用，不经过本接口。
 * <p>
 * 校验实现已按章节维度落地（命中率 + 是否贴合 + 教材原文片段 + 修改建议），
 * 取代第一次任务的"全局关键词包含匹配"硬编码版本。
 */
@RestController
@RequestMapping("/api/knowledge/verify")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class KnowledgeVerifyController {

    private final KnowledgeChapterApi knowledgeChapterApi;

    /**
     * 校验 AI 生成内容是否贴合指定章节教材
     * <p>
     * POST /api/knowledge/verify/content
     */
    @PostMapping("/content")
    public Result<KnowledgeVerifyResponse> verify(@Valid @RequestBody KnowledgeVerifyRequest request) {
        return knowledgeChapterApi.verify(request);
    }
}
