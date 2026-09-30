package com.amlve.exam.controller;

import com.amlve.common.request.Result;
import com.amlve.exam.dto.response.GradingResultResponse;
import com.amlve.exam.service.ExamGradingService;
import com.amlve.exam.util.ExamAuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 批改结果查询接口。
 * <p>
 * 接口级 {@code @PreAuthorize("isAuthenticated()")} 要求登录；
 * 学生仅能查询本人的批改结果（按当前登录用户过滤）。
 */
@RestController
@RequestMapping("/api/exam/grading")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ExamGradingController {

    private final ExamGradingService gradingService;
    private final ExamAuthSupport examAuthSupport;

    /** 查询本人某次作业的批改结果（要求已登录） */
    @GetMapping("/result/{homeworkId}")
    public Result<GradingResultResponse> result(@PathVariable Long homeworkId) {
        Long userId = examAuthSupport.requireLogin();
        return Result.success(gradingService.getByHomeworkAndUser(homeworkId, userId));
    }
}
