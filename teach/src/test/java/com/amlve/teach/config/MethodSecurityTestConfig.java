package com.amlve.teach.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 测试专用安全配置。
 * teach 模块自身不定义 SecurityConfig（全局配置在 bootstrap），
 * 因此在切片测试中单独开启方法级安全，使 @PreAuthorize 生效。
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity
public class MethodSecurityTestConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // 测试环境关闭 CSRF，方便 POST
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                // 未认证返回 401（与生产 JWT 过滤器行为一致）；权限不足仍为 403
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (request, response, ex) -> response.sendError(401)));
        return http.build();
    }
}
