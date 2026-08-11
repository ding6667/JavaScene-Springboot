package com.amlve.system.controller;

import com.amlve.common.request.Result;
import com.amlve.system.dto.RegisterRequest;
import com.amlve.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/register")
public class RegisterController {
    private final UserService userService;

    /**
     * 注册
     * @param dto
     * @return
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterRequest registerRequest) {
        return Result.success(userService.register(registerRequest.getUsername(),registerRequest.getPassword()));
    }
}
