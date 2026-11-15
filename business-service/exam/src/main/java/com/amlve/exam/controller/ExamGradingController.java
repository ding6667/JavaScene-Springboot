package com.amlve.exam.controller;

import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.exam.dto.response.GradingResultResponse;
import com.amlve.exam.service.ExamGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 批改结果查询接口。
 * <p>
 * 类级 {@code @PreAuthorize("isAuthenticated()")} 要求登录。
 */
@RestController
@RequestMapping("/api/exam/grading")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ExamGradingController {

    private final ExamGradingService gradingService;

    /** 查询当前登录学生某次作业的批改结果 */
    @GetMapping("/result/{homeworkId}")
    public Result<GradingResultResponse> result(@PathVariable Long homeworkId) {
        Long userId = SecurityUtils.getUserId();
        return Result.success(gradingService.getByHomeworkAndUser(homeworkId, userId));
    }
}
