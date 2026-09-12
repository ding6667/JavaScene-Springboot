package com.amlve.study.controller;

import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.study.dto.request.StudyProgressSaveRequest;
import com.amlve.study.dto.response.StudyProgressResponse;
import com.amlve.study.service.StudyProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学习进度管理
 */
@RestController
@RequestMapping("/api/study/progress")
@RequiredArgsConstructor
public class StudyProgressController {

    private final StudyProgressService studyProgressService;

    /** 保存当前登录学生的学习进度 */
    @PostMapping("/save")
    public Result<Long> saveStudyProgress(@Valid @RequestBody StudyProgressSaveRequest request) {
        // 获取当前登录学生的id
        Long userId = SecurityUtils.getUserId();
        return Result.success(studyProgressService.saveProgress(userId, request));
    }

    /** 查询当前登录学生在指定课程下的所有章节学习进度（未学章节进度为 0） */
    @GetMapping("/get")
    public Result<List<StudyProgressResponse>> getStudyProgress(@RequestParam Long courseId) {
        Long userId = SecurityUtils.getUserId();
        return Result.success(studyProgressService.listProgressByUser(userId, courseId));
    }
}
