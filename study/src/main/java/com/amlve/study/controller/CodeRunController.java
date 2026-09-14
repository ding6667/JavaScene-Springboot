package com.amlve.study.controller;

import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.study.dto.request.CodeRunRequest;
import com.amlve.study.dto.response.CodeRunResultResponse;
import com.amlve.study.service.CodeRunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生代码编译运行（学习场景下编译运行 Java 代码）
 * <p>
 * 端点：POST /api/study/code/run
 * 鉴权：必须登录（从 Token 解析 userId）
 * 约束：需要一个类作为主类（默认 {@code public class Main}），并包含
 * {@code public static void main(String[] args)} 方法
 */
@RestController
@RequestMapping("/api/study/code")
@RequiredArgsConstructor
public class CodeRunController {

    private final CodeRunService codeRunService;

    /**
     * 学生提交 Java 代码，后端编译并运行后返回结果。
     */
    @PostMapping("/run")
    public Result<CodeRunResultResponse> run(@Valid @RequestBody CodeRunRequest request) {
        Long userId = SecurityUtils.getUserId();
        return Result.success(codeRunService.run(userId, request));
    }
}
