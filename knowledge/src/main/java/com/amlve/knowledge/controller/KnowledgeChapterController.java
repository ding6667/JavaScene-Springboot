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
 * <p>
 * 章节目录是三类角色共用的只读导航数据（学生看进度、教师选题、管理员总览），
 * 无角色专属信息，故维持登录级校验 isAuthenticated()，不做角色细分。
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
