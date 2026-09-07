package com.amlve.knowledge.controller;

import com.amlve.common.request.Result;
import com.amlve.knowledge.dto.response.ChapterResponse;
import com.amlve.knowledge.service.KnowledgeChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 章节接口
 */
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
}
