package com.amlve.teach.service.impl;

import com.amlve.common.security.UserIdAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 测试专用：向 SecurityContextHolder 注入/清除登录态。
 * 与运行时 JwtAuthenticationFilter 构建的认证结构保持一致。
 */
final class SecurityContextHolderTestUtil {

    private SecurityContextHolderTestUtil() {
    }

    /** 模拟指定 userId 的登录用户 */
    static void loginAs(Long userId, String username) {
        UserIdAuthenticationToken authentication =
                new UserIdAuthenticationToken(userId, username, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** 清除登录态，避免测试间互相污染 */
    static void logout() {
        SecurityContextHolder.clearContext();
    }
}
