package com.amlve.gateway.filter;

import com.amlve.common.constant.AuthHeaderConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.common.security.token.TokenRevocationService;
import com.amlve.common.utils.JWTUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * 网关鉴权：验签 JWT、查吊销状态，然后把身份写进请求头转发给下游。
 * 下游服务不再验签，只信这三个头，所以入口必须先剥掉客户端自带的同名头。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty("jwt.secret")
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JWTUtil jwtUtil;
    private final TokenRevocationService tokenRevocationService;
    private final ObjectMapper objectMapper;

    /** 免鉴权路径，Ant 风格，逗号分隔；登录注册等公开接口配在这里 */
    @Value("${com.amlve.gateway.public-paths:}")
    private List<String> publicPaths;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public int getOrder() {
        // 要早于路由转发，才能在放行前改好请求头
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 先无条件剥离，防止外部伪造身份头绕过鉴权
        ServerWebExchange stripped = exchange.mutate()
                .request(builder -> builder.headers(this::removeIdentityHeaders))
                .build();

        if (isPublic(path)) {
            return chain.filter(stripped);
        }

        String token = jwtUtil.extractToken(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            return reject(stripped, 401, "未登录或凭证缺失");
        }

        Claims claims;
        Long userId;
        try {
            claims = jwtUtil.parseClaims(token);
            userId = Long.valueOf(claims.getSubject());
        } catch (BusinessException e) {
            return reject(stripped, e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("网关 JWT 解析失败: {}", e.getMessage());
            return reject(stripped, 401, "Token 无效");
        }

        long issuedAt = claims.getIssuedAt().getTime() / 1000;
        String jti = claims.getId();
        // 吊销检查走同步 Redis，挪到弹性线程池，别堵住 Netty 事件循环
        return Mono.fromCallable(() -> tokenRevocationService.isRevoked(jti, userId, issuedAt))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("吊销检查失败，按未吊销处理, userId={}", userId, e);
                    return Mono.just(false); // 或者按安全策略返回 true 拒绝
                })
                .flatMap(revoked -> {
                    if (revoked) {
                        return reject(stripped, 401, "登录状态已失效，请重新登录");
                    }
                    return chain.filter(withIdentity(stripped, userId, claims));
                });
    }

    private ServerWebExchange withIdentity(ServerWebExchange exchange, Long userId, Claims claims) {
        List<String> roles = claims.get("roles", List.class);
        String joined = roles == null ? "" : String.join(",", roles);
        String requestId = resolveRequestId(exchange);

        ServerWebExchange mutate = exchange.mutate()
                .request(builder -> {
                    builder.header(AuthHeaderConstant.USER_ID, String.valueOf(userId));
                    builder.header(AuthHeaderConstant.USER_NAME, String.valueOf(claims.get("username")));
                    builder.header(AuthHeaderConstant.USER_ROLES, joined);
                    builder.header(AuthHeaderConstant.REQUEST_ID, requestId);
                })
                .build();
        mutate.getResponse().getHeaders().set(AuthHeaderConstant.REQUEST_ID,requestId);
        return mutate;
    }

    private String resolveRequestId(ServerWebExchange exchange) {
        String requestId = exchange.getRequest().getHeaders().getFirst(AuthHeaderConstant.REQUEST_ID);
        if (requestId != null && !requestId.isEmpty()) {
            return requestId;
        }
        return UUID.randomUUID().toString().substring(0, 12);
    }

    private void removeIdentityHeaders(HttpHeaders headers) {
        headers.remove(AuthHeaderConstant.USER_ID);
        headers.remove(AuthHeaderConstant.USER_NAME);
        headers.remove(AuthHeaderConstant.USER_ROLES);
    }

    private boolean isPublic(String path) {
        return publicPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern.trim(), path));
    }

    /**
     * 与单体时保持一致：HTTP 200 + 业务码，前端统一按 code 分支处理
     */
    private Mono<Void> reject(ServerWebExchange exchange, int code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsString(Result.error(code, message)).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            body = ("{\"code\":" + code + ",\"msg\":\"" + message + "\",\"data\":null}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
