package com.amlve.exam.controller;

import com.amlve.common.request.Result;
import com.amlve.exam.dto.response.DifficultyConfigResponse;
import com.amlve.exam.service.ExamDifficultyConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 出题难度配置查询接口（教师、管理员）：题库难度已改为指向本表的逻辑外键，
 * 前端出题、组卷、题库筛选都先取这里拿可选难度。
 */
@RestController
@RequestMapping("/api/exam/difficulty-config")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
public class ExamDifficultyConfigController {

    private final ExamDifficultyConfigService difficultyConfigService;

    /**
     * 查询可用难度配置（启用中的系统预设与本人自建，按难度从易到难）。
     *
     * @param chapterId 章节ID，可选；传入时附带该章节的专属配置
     */
    @GetMapping("/list")
    public Result<List<DifficultyConfigResponse>> list(@RequestParam(required = false) Long chapterId) {
        return Result.success(difficultyConfigService.listUsable(chapterId));
    }
}
