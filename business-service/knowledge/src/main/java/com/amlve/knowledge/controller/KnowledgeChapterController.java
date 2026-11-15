package com.amlve.knowledge.controller;

import com.amlve.common.request.Result;
import com.amlve.knowledge.dto.response.ChapterResponse;
import com.amlve.knowledge.service.KnowledgeChapterService;
import com.amlve.knowledgeapi.dto.request.KnowledgeChapterSaveRequest;
import com.amlve.knowledgeapi.dto.response.ChapterTreeDTO;
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
 * 章节接口
 * <p>
 * 章节目录是三类角色共用的只读导航数据（学生看进度、教师选题、管理员总览），
 * 无角色专属信息，故查询维持登录级校验 isAuthenticated()；
 * 目录维护属运维职责，单独限 ADMIN。
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge/chapter")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class KnowledgeChapterController {

    private final KnowledgeChapterService knowledgeChapterService;

    /**
     * 按课程查询章节列表
     * <p>
     * GET /api/knowledge/chapter/list?courseId=1
     */
    @GetMapping("/list")
    public Result<List<ChapterResponse>> listByCourse(@RequestParam Long courseId) {
        return Result.success(knowledgeChapterService.listByCourse(courseId));
    }

    /**
     * 按课程查询章节树（父子层级组装，含子节点）
     * <p>
     * GET /api/knowledge/chapter/tree?courseId=1
     */
    @GetMapping("/tree")
    public Result<List<ChapterTreeDTO>> tree(@RequestParam Long courseId) {
        return Result.success(knowledgeChapterService.chapterTree(courseId));
    }

    /**
     * 新增或更新章节（教材目录维护，仅管理员）
     * <p>
     * POST /api/knowledge/chapter/save
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public Result<Long> save(@Valid @RequestBody KnowledgeChapterSaveRequest request) {
        log.info("保存章节: courseId={}, chapterCode={}, id={}",
                request.getCourseId(), request.getChapterCode(), request.getId());
        return Result.success(knowledgeChapterService.saveChapter(request));
    }
}
