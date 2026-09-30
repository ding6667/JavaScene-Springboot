package com.amlve.exam.controller;

import com.amlve.common.request.Result;
import com.amlve.exam.dto.request.HomeworkSubmitRequest;
import com.amlve.exam.service.ExamHomeworkService;
import com.amlve.exam.util.ExamAuthSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
// TODO 缺少@PreAuthorize注解
// TODO 缺少javadoc注释

/**
 * 作业提交接口。
 * <p>
 * 接口级 {@code @PreAuthorize("isAuthenticated()")} 要求登录；
 * 学生登录后提交答案，提交成功即触发异步批改（提交方即当前登录用户）。
 */
@RestController
@RequestMapping("/api/exam/homework")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ExamHomeworkController {

    private final ExamHomeworkService homeworkService;
    private final ExamAuthSupport examAuthSupport;

    /** 学生提交作业答案（要求已登录） */
    @PostMapping("/submit")
    public Result<Void> submit(@Valid @RequestBody HomeworkSubmitRequest request) {
        examAuthSupport.requireLogin();
        homeworkService.submit(request);
        return Result.success(null);
    }
}
