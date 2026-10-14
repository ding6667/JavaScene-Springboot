package com.amlve.knowledge.controller;

import com.amlve.common.request.Result;
import com.amlve.knowledge.service.KnowledgePointService;
import com.amlve.knowledgeapi.dto.request.KnowledgePointSaveRequest;
import com.amlve.knowledgeapi.dto.response.KnowledgePointDTO;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识点维护接口
 * <p>
 * 权限口径：维护限 ADMIN，查询面向所有登录用户。
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge/point")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class KnowledgePointController {

    private final KnowledgePointService knowledgePointService;

    private final KnowledgeChapterApi knowledgeChapterApi;

    /**
     * 按章节查询知识点列表
     * <p>
     * GET /api/knowledge/point/list?chapterId=5
     */
    @GetMapping("/list")
    public Result<List<KnowledgePointDTO>> list(@RequestParam Long chapterId) {
        return knowledgeChapterApi.listPointsByChapter(chapterId);
    }

    /**
     * 新增或更新知识点
     * <p>
     * POST /api/knowledge/point/save
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public Result<Long> save(@Valid @RequestBody KnowledgePointSaveRequest request) {
        log.info("保存知识点: chapterId={}, name={}, id={}",
                request.getChapterId(), request.getName(), request.getId());
        return Result.success(knowledgePointService.savePoint(request));
    }
}
