package com.amlve.knowledge.controller;

import com.amlve.common.request.Result;
import com.amlve.knowledge.dto.request.VerifyContentRequest;
import com.amlve.knowledge.dto.response.VerifyResponse;
import com.amlve.knowledge.service.KnowledgeDatasetService;
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
 * 校验用于教材入库前的关键词把关，属教师侧运维能力，故限 TEACHER / ADMIN；
 * teach 等模块的服务内调用走 KnowledgeDatasetService，不经过本接口。
 */
@RestController
@RequestMapping("/api/knowledge/verify")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class KnowledgeVerifyController {

    private final KnowledgeDatasetService knowledgeDatasetService;

    /**
     * 校验内容是否包含教材关键词
     * <p>
     * POST /api/knowledge/verify/content
     */
    @PostMapping("/content")
    public Result<VerifyResponse> verifyContent(@Valid @RequestBody VerifyContentRequest request) {
        return Result.success(knowledgeDatasetService.verifyContent(request.getContent()));
    }
}
