package com.amlve.system.controller;

import com.amlve.common.request.Result;
import com.amlve.system.dto.request.LoginRequest;
import com.amlve.system.dto.request.LogoutRequest;
import com.amlve.system.dto.response.LoginVO;
import com.amlve.system.service.TokenRevocationService;
import com.amlve.system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/auth")
public class LoginController {

    private final UserService userService;
    private final TokenRevocationService tokenRevocationService;

    /**
     * 登录：返回 token + 角色 + 后端算好的落地页
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest loginRequest) {
        return Result.success(userService.checkUser(loginRequest.getUsername(), loginRequest.getPassword()));
    }

    /**
     * 登出。幂等：token 缺失、伪造、已过期一律返回成功——「让我下次用不了这个 token」这个目标
     * 在 token 本来就不可用时已经达成，返回失败只会让前端卡住。
     * 只有 Redis 写入异常才向上抛（由兜底处理器转成 500），因为那意味着吊销确实没生效。
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                               @RequestBody(required = false) LogoutRequest logoutRequest) {
        boolean allDevices = logoutRequest != null && Boolean.TRUE.equals(logoutRequest.getAllDevices());
        tokenRevocationService.logout(authHeader, allDevices);
        return Result.success(null);
    }
}