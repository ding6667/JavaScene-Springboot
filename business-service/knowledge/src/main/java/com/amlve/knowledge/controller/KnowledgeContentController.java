package com.amlve.knowledge.controller;

import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.knowledge.service.KnowledgeContentService;
import com.amlve.knowledgeapi.dto.request.KnowledgeContentQueryRequest;
import com.amlve.knowledgeapi.dto.request.KnowledgeContentSaveRequest;
import com.amlve.knowledgeapi.dto.response.KnowledgeContentVO;
import com.amlve.knowledgeapi.service.KnowledgeContentApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教材原文内容维护接口
 * <p>
 * 权限口径：维护（新增/更新/删除）限 ADMIN（运维导入教材素材），
 * 查询面向所有登录用户（教师选题、学生看素材）。
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge/content")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class KnowledgeContentController {

    private final KnowledgeContentService knowledgeContentService;

    private final KnowledgeContentApi knowledgeContentApi;

    /**
     * 按章节分页查询教材原文
     * <p>
     * GET /api/knowledge/content/list?chapterId=5&contentType=1&keyword=构造器&pageNum=1&pageSize=10
     */
    @GetMapping("/list")
    public Result<PageResult<KnowledgeContentVO>> list(@Valid KnowledgeContentQueryRequest request) {
        return knowledgeContentApi.listContents(request);
    }

    /**
     * 新增或更新教材原文（同步 Dify 知识库）
     * <p>
     * POST /api/knowledge/content/save
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public Result<Long> save(@Valid @RequestBody KnowledgeContentSaveRequest request) {
        log.info("保存教材原文: chapterId={}, contentType={}, page={}, id={}",
                request.getChapterId(), request.getContentType(), request.getPage(), request.getId());
        return Result.success(knowledgeContentService.saveContent(request));
    }

    /**
     * 逻辑删除教材原文（同时归档删除 Dify 文档）
     * <p>
     * PUT /api/knowledge/content/delete?id=1
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        knowledgeContentService.deleteContent(id);
        return Result.success(null);
    }
}
