package com.amlve.exam.controller;

import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.exam.dto.response.GradingResultResponse;
import com.amlve.exam.service.ExamGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exam/grading")
@RequiredArgsConstructor
public class ExamGradingController {

    private final ExamGradingService gradingService;

    @GetMapping("/result/{homeworkId}")
    public Result<GradingResultResponse> result(@PathVariable Long homeworkId) {
        Long userId = SecurityUtils.getUserId();
        return Result.success(gradingService.getByHomeworkAndUser(homeworkId, userId));
    }
}