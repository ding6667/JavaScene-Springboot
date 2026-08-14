package com.amlve.core.filter;

import com.amlve.common.configuration.UserIdAuthenticationToken;
import com.amlve.common.utils.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final List<String> WHITE_LIST = Arrays.asList("/api/auth/login", "/api/auth/register");
    private final JWTUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        // 如果是白名单路径，直接放行（不解析 Token）
        if (WHITE_LIST.stream().anyMatch(uri::contains)) {
            chain.doFilter(request, response);
            return;
        }
        String token = extractToken(request);

        if (token != null && jwtUtil.verifyToken(token)) {
            Long userId = jwtUtil.getUserIdByToken(token);
            String username = jwtUtil.getUsernameByToken(token);

            // 构造自定义 Authentication（这里暂时不添加权限，或可以添加默认角色）
            UserIdAuthenticationToken authentication =
                    new UserIdAuthenticationToken(userId, username, null);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}