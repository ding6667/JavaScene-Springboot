package com.amlve.common.security.filter;

import com.amlve.common.constant.AuthHeaderConstant;
import com.amlve.common.security.UserIdAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 下游服务的鉴权入口：不再验签 JWT，只信任网关转发的身份头。
 * 角色来自 token 的 roles claim，由网关解析后原样透传，
 * 这里补上 ROLE_ 前缀，使各服务现有的 @PreAuthorize 与 SecurityUtils 行为不变。
 */
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // SSE / @Async 的异步派发同样要带上身份，否则流式接口拿不到用户
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String userId = request.getHeader(AuthHeaderConstant.USER_ID);
        if (StringUtils.hasText(userId) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long id = Long.valueOf(userId.trim());
                String username = request.getHeader(AuthHeaderConstant.USER_NAME);
                SecurityContextHolder.getContext().setAuthentication(
                        new UserIdAuthenticationToken(id, username, parseAuthorities(
                                request.getHeader(AuthHeaderConstant.USER_ROLES))));
            } catch (NumberFormatException e) {
                log.warn("网关转发的用户标识无法解析: {}", userId);
            }
        }
        chain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parseAuthorities(String roles) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (!StringUtils.hasText(roles)) {
            return authorities;
        }
        for (String role : roles.split(",")) {
            if (StringUtils.hasText(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.trim()));
            }
        }
        return authorities;
    }
}
