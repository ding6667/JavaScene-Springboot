package com.amlve.knowledge.controller;

import com.amlve.common.request.Result;
import com.amlve.knowledge.dto.request.UpdateMetadataRequest;
import com.amlve.knowledge.dto.request.UploadTextRequest;
import com.amlve.knowledge.dto.response.KnowledgeResponse;
import com.amlve.knowledge.dto.response.UploadResponse;
import com.amlve.knowledge.service.KnowledgeDatasetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库数据集接口（检索 / 上传 / 元数据）
 */
// TODO 缺少@PreAuthorize注解，@PreAuthorize("isAuthenticated()") 仅说明用户登录，但无法证明是否有角色权限访问，需在对应方法上添加@PreAuthorize("hasRole('')"注解
// TODO 缺少学生提问流式输出接口
//  GET /api/knowledge/dataset/querySSE?keywords=详细讲解为什么数组索引从0开始
//   需配合qa模块的answer方法使用，返回流式输出的响应

@RestController
@RequestMapping("/api/knowledge/dataset")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class KnowledgeDatasetController {

    private final KnowledgeDatasetService knowledgeDatasetService;

    /**
     * 知识点检索（Dify 混合检索）
     * <p>
     * GET /api/knowledge/dataset/query?keywords=什么是面向对象
     */
    @GetMapping("/query")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<KnowledgeResponse> query(@RequestParam String keywords) {
        return Result.success(knowledgeDatasetService.query(keywords));
    }

    /**
     * 上传文本文档（运维用）
     * <p>
     * POST /api/knowledge/dataset/upload/text
     */
    @PostMapping("/upload/text")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UploadResponse> uploadText(@Valid @RequestBody UploadTextRequest request) {
        return Result.success(knowledgeDatasetService.uploadText(request));
    }

    /**
     * 上传文件文档（运维用）
     * <p>
     * POST /api/knowledge/dataset/upload/file?chapterId=3，form-data: file
     */
    @PostMapping("/upload/file")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UploadResponse> uploadFile(@RequestParam Long chapterId,
                                             @RequestParam("file") MultipartFile file) {
        return Result.success(knowledgeDatasetService.uploadFile(chapterId, file));
    }

    /**
     * 更新文档元数据（运维用）
     * <p>
     * POST /api/knowledge/dataset/metadata/update
     */
    @PostMapping("/metadata/update")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateMetadata(@Valid @RequestBody UpdateMetadataRequest request) {
        return Result.success(knowledgeDatasetService.updateMetadata(request));
    }
}
