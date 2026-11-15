package com.amlve.system.controller;

import com.amlve.system.service.impl.UserServiceImpl;
import com.amlve.systemapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UserService（system-api 契约）的 HTTP 提供端：Feign 客户端按同一接口调用，方法映射继承自接口。
 * <p>
 * 与模块内部同名接口 {@code com.amlve.system.service.UserService} 无关，此处固定引用 system-api 的那个。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/system/user-service")
public class UserServiceController implements UserService {

    private final UserServiceImpl userService;

    @Override
    public boolean isTeacher(Long userId) {
        return userService.isTeacher(userId);
    }

    @Override
    public boolean isAdmin(Long userId) {
        return userService.isAdmin(userId);
    }
}
