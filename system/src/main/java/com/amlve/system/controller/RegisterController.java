package com.amlve.system.controller;

import com.amlve.common.request.Result;
import com.amlve.system.dto.request.AdminRegisterRequest;
import com.amlve.system.dto.request.RegisterRequest;
import com.amlve.system.dto.response.LoginResponse;
import com.amlve.system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/auth")
public class RegisterController {
    private final UserService userService;

    /**
     * 学生自助注册，注册即登录
     */
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return Result.success(userService.register(registerRequest.getUsername(), registerRequest.getPassword()));
    }

    /**
     * 管理员注册（系统后台，需初始化密钥 app.admin.init-key，注册即登录）
     */
    @PostMapping("/admin/register")
    public Result<LoginResponse> registerAdmin(@Valid @RequestBody AdminRegisterRequest request) {
        return Result.success(userService.registerAdmin(request));
    }
}