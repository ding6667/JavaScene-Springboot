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
 */
// TODO 缺少@PreAuthorize注解，@PreAuthorize("isAuthenticated()") 仅说明用户登录，但无法证明是否有角色权限访问，需在对应方法上添加@PreAuthorize("hasRole('')"注解
@RestController
@RequestMapping("/api/knowledge/verify")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
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
