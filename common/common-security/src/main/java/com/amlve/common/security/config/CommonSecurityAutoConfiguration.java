package com.amlve.common.security.config;

import com.amlve.common.constant.AuthHeaderConstant;
import com.amlve.common.security.filter.HeaderAuthenticationFilter;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

/**
 * 下游业务服务的统一安全配置（只在 Servlet 栈生效，网关是 Reactive 不会加载）。
 * 身份来自 {@link AuthHeaderConstant} 定义的请求头，本服务不再持有 JWT 密钥。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(SecurityFilterChain.class)
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class CommonSecurityAutoConfiguration {

    /**
     * 免鉴权路径，逗号分隔。各服务按自身对外接口在 application.yml 里覆盖。
     */
    @Value("${com.amlve.security.exclude-paths:/api/user/auth/**}")
    private String excludePaths;

    @PostConstruct
    public void warnIfNoExcludePaths() {
        String[] paths = StringUtils.commaDelimitedListToStringArray(excludePaths);
        for (String path : paths) {
            if (!path.startsWith("/")) {
                throw new IllegalStateException("com.amlve.security.exclude-paths 必须以 / 开头: " + path);
            }
        }
    }

    @Bean
    public HeaderAuthenticationFilter headerAuthenticationFilter() {
        return new HeaderAuthenticationFilter();
    }

    /**
     * 过滤器只交给 Security 链，禁止 Servlet 容器再自动注册一次
     */
    @Bean
    public FilterRegistrationBean<HeaderAuthenticationFilter> disableHeaderFilterAutoRegistration(
            HeaderAuthenticationFilter filter) {
        FilterRegistrationBean<HeaderAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * 权限不足仍返回 HTTP 200、业务码 403：前端统一按 code 分支处理，
     * 改成真 403 会让现有页面全部走进 fetch 的异常分支
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"code\":403,\"msg\":\"权限不足，无法访问该资源\",\"data\":null}");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   HeaderAuthenticationFilter headerAuthenticationFilter,
                                                   AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AuthHeaderConstant.INTERNAL_PATH + "/**").permitAll()
                        .requestMatchers(StringUtils.commaDelimitedListToStringArray(excludePaths)).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * system 服务登录用：Boot 会把容器里唯一的 UserDetailsService 接进这个 Manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
