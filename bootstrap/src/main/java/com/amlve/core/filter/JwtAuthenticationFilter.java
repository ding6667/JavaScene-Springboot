package com.amlve.core.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.amlve.common.security.UserIdAuthenticationToken;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.common.utils.JWTUtil;
import com.amlve.system.service.impl.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final List<String> WHITE_LIST = Arrays.asList("/api/user/auth/login", "/api/user/auth/register");
    private final JWTUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (WHITE_LIST.stream().anyMatch(uri::contains)) {
            chain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);

        if (token != null) {
            try {
                if (jwtUtil.verifyToken(token)) {
                    Long userId = jwtUtil.getUserIdByToken(token);
                    String username = jwtUtil.getUsernameByToken(token);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UserIdAuthenticationToken authentication =
                            new UserIdAuthenticationToken(userDetails, userId, username);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (BusinessException e) {
                log.warn("JWT 验证失败: {}", e.getMessage());
                writeErrorResponse(response, e.getCode(), e.getMessage());
                return;
            } catch (Exception e) {
                log.error("JWT 解析异常: ", e);
                writeErrorResponse(response, 401, "Token 无效");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void writeErrorResponse(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> errorResult = Result.error(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResult));
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}