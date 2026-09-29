package com.amlve.systemapi.service;

import com.amlve.common.request.Result;

/**
 * 用户设置对外服务接口（供其他模块调用）
 * <p>
 * 跨模块只允许依赖 system-api 的接口，禁止注入 system 实现类。
 */
public interface SettingsService {

    /**
     * 获取指定用户加密存储的 API 密钥
     *
     * @param userId 用户ID
     * @return 解密后的 API 密钥明文
     */
    Result<String> getEncryptedAPIKey(Long userId);
}
