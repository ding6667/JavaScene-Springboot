package com.amlve.study.controller;

import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.study.dto.request.CodeRunRequest;
import com.amlve.study.dto.response.CodeRunResultResponse;
import com.amlve.study.service.CodeRunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生代码编译运行（学习场景下编译运行 Java 代码）
 * <p>
 * 端点：POST /api/study/code/run、POST /api/study/code/debug
 * 鉴权：仅学生可调用（userId 从 Token 解析）
 * 约束：需要一个类作为主类（默认 {@code public class Main}），并包含
 * {@code public static void main(String[] args)} 方法
 */
@Slf4j
@RestController
@RequestMapping("/api/study/code")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class CodeRunController {

    private final CodeRunService codeRunService;

    /**
     * 学生提交 Java 代码，后端编译并运行后返回结果。
     * 出错（编译/运行时错误）时自动调用 AI 调试智能体返回建议。
     */
    @PostMapping("/run")
    public Result<CodeRunResultResponse> run(@Valid @RequestBody CodeRunRequest request) {
        Long userId = SecurityUtils.getUserId();
        log.info("学生运行代码: userId={}, chapterId={}, questionId={}",
                userId, request.getChapterId(), request.getQuestionId());
        return Result.success(codeRunService.run(userId, request));
    }

    /**
     * 调试模式：编译运行后无论成败都调用代码调试智能体，
     * 返回错误原因、优化建议、标准解题思路与关联知识点。
     */
    @PostMapping("/debug")
    public Result<CodeRunResultResponse> debug(@Valid @RequestBody CodeRunRequest request) {
        Long userId = SecurityUtils.getUserId();
        log.info("学生调试代码: userId={}, chapterId={}, questionId={}",
                userId, request.getChapterId(), request.getQuestionId());
        return Result.success(codeRunService.debugRun(userId, request));
    }
}
