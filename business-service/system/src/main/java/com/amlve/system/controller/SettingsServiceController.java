package com.amlve.system.controller;

import com.amlve.common.request.Result;
import com.amlve.system.service.impl.SettingsServiceImpl;
import com.amlve.systemapi.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SettingsService（system-api 契约）的 HTTP 提供端：Feign 客户端按同一接口调用，方法映射继承自接口。
 * <p>
 * 与模块内部同名接口 {@code com.amlve.system.service.SettingsService} 无关，此处固定引用 system-api 的那个。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/system/settings")
public class SettingsServiceController implements SettingsService {

    private final SettingsServiceImpl settingsService;

    @Override
    public Result<String> getEncryptedAPIKey(Long userId) {
        return settingsService.getEncryptedAPIKey(userId);
    }
}
