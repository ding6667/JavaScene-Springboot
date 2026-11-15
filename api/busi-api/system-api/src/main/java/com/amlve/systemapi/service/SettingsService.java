package com.amlve.systemapi.service;

import com.amlve.common.request.Result;
import com.amlve.common.web.feign.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户设置对外服务接口（供其他模块调用）
 * <p>
 * 跨模块只允许依赖 system-api 的接口，禁止注入 system 实现类。
 */
@FeignClient(name = "system-service", contextId = "settingsService", path = "/internal/system/settings", configuration = FeignConfig.class)
public interface SettingsService {

    /**
     * 获取指定用户加密存储的 API 密钥
     *
     * @param userId 用户ID
     * @return 解密后的 API 密钥明文
     */
    @PostMapping("/getEncryptedAPIKey")
    Result<String> getEncryptedAPIKey(@RequestParam("userId") Long userId);
}
