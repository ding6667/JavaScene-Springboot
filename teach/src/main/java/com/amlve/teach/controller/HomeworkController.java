package com.amlve.teach.controller;

import com.amlve.common.request.Result;
import com.amlve.teach.dto.request.HomeworkCloseRequest;
import com.amlve.teach.dto.request.HomeworkPublishRequest;
import com.amlve.teach.dto.response.HomeworkDetailResponse;
import com.amlve.teach.dto.response.HomeworkResponse;
import com.amlve.teach.service.TeachService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 作业管理接口（教师端）
 */
@RestController
@RequestMapping("/api/teach/homework")
@RequiredArgsConstructor
public class HomeworkController {

    private final TeachService teachService;

    /** 教师发布作业 */
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/publish")
    public Result<Long> publish(@Valid @RequestBody HomeworkPublishRequest request) {
        return Result.success(teachService.publishHomework(request));
    }

    /** 教师查询自己发布的作业列表 */
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/list")
    public Result<List<HomeworkResponse>> list() {
        return Result.success(teachService.listMyHomework());
    }

    /** 教师查询作业详情（含题目完整快照），非本人作业拒绝访问 */
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/detail")
    public Result<HomeworkDetailResponse> detail(@RequestParam("homeworkId") Long homeworkId) {
        return Result.success(teachService.getMyHomeworkDetail(homeworkId));
    }

    /** 教师关闭已发布的作业（截止收题） */
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/close")
    public Result<Void> close(@Valid @RequestBody HomeworkCloseRequest request) {
        teachService.closeHomework(request);
        return Result.success(null);
    }
}
