package com.amlve.core.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.security.UserIdAuthenticationToken;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.common.utils.JWTUtil;
import com.amlve.system.service.TokenRevocationService;
import com.amlve.system.service.impl.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
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
    /**
     * 免鉴权路径。logout 必须在内：登出对无效/过期 token 也要能进来做幂等处理，
     * 否则 Spring Security 会先拒绝，前端拿不到 200 就卡住
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/user/auth/login", "/api/user/auth/register", "/api/user/auth/logout");
    private final JWTUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenRevocationService tokenRevocationService;

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
                    // 一次解析取齐 subject / jti / iat，不再逐字段重复验签
                    Claims claims = jwtUtil.parseClaims(token);
                    Long userId = Long.valueOf(claims.getSubject());
                    String username = claims.get("username", String.class);

                    // 吊销校验放在装载 authorities 之前：命中黑名单或签发早于用户下线时刻，
                    // 就根本不给出身份，后续 @PreAuthorize 与业务层都无从放行
                    if (tokenRevocationService.isRevoked(claims.getId(), userId,
                            claims.getIssuedAt().getTime() / 1000)) {
                        writeErrorResponse(response, ErrorCodeConstant.UNAUTHORIZED,
                                ErrorMessageConstant.TOKEN_REVOKED);
                        return;
                    }

                    // 每次请求按用户名加载 UserDetails，其 authorities 已含 ROLE_<roleName>，
                    // 因此 @PreAuthorize 的 hasRole() 可直接生效
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // 登录时已禁用的账号拿不到 token，但「先登录、后被禁用」的存量 token
                    // 在过期前仍然有效，必须在这里按数据库的 enabled 再拦一次
                    if (!userDetails.isEnabled()) {
                        writeErrorResponse(response, ErrorCodeConstant.UNAUTHORIZED,
                                ErrorMessageConstant.ACCOUNT_DISABLED);
                        return;
                    }

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