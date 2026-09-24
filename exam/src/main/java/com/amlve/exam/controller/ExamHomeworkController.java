package com.amlve.exam.controller;

import com.amlve.common.request.Result;
import com.amlve.exam.dto.request.HomeworkSubmitRequest;
import com.amlve.exam.service.ExamHomeworkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exam/homework")
@RequiredArgsConstructor
public class ExamHomeworkController {

    private final ExamHomeworkService homeworkService;

    @PostMapping("/submit")
    public Result<Void> submit(@Valid @RequestBody HomeworkSubmitRequest request) {
        homeworkService.submit(request);
        return Result.success(null);
    }
}