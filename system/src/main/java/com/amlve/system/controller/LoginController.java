package com.amlve.system.controller;

import com.amlve.common.request.Result;
import com.amlve.system.dto.LoginRequest;
import com.amlve.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final UserService userService;

    /**
     * 登录
     * @param loginRequest
     * @return
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginRequest loginRequest) {
        String token = userService.checkUser(loginRequest.getUsername(), loginRequest.getPassword());
        return Result.success(token);
    }
}
