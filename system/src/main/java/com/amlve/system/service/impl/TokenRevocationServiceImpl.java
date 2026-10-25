package com.amlve.system.service.impl;

import com.amlve.common.utils.JWTUtil;
import com.amlve.system.service.TokenRevocationService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationServiceImpl implements TokenRevocationService {

    /**
     * 单 token 黑名单：键存在即失效，value 无意义
     */
    private static final String BLACKLIST_KEY = "auth:blacklist:";

    /**
     * 用户级下线时刻：value 为 epoch 秒，早于该时刻签发的 token 一律失效
     */
    private static final String KICKED_KEY = "auth:kicked:";

    private final JWTUtil jwtUtil;
    private final StringRedisTemplate redis;

    @Override
    public void logout(String authorizationHeader, boolean allDevices) {
        String token = jwtUtil.extractToken(authorizationHeader);
        if (token == null || token.isBlank()) {
            log.debug("登出请求未携带 token，按幂等处理");
            return;
        }

        Claims claims;
        try {
            claims = jwtUtil.parseIgnoringExpiration(token);
        } catch (Exception e) {
            // 验签失败说明 token 本就不可用，吊销无意义；这里不能放宽为跳过验签，
            // 否则伪造 token 也能向 Redis 写键，变成廉价的拒绝服务入口
            log.debug("登出时 token 验签失败，不做吊销: {}", e.getMessage());
            return;
        }

        String jti = claims.getId();
        String userId = claims.getSubject();
        long remainSeconds = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;

        // TTL 取 token 剩余寿命：写长了白占内存，写短了黑名单提前失效留下漏洞。
        // 已过期（remainSeconds <= 0）无需拉黑，本来就验不过
        if (jti != null && remainSeconds > 0) {
            redis.opsForValue().set(BLACKLIST_KEY + jti, "1", Duration.ofSeconds(remainSeconds));
        }

        if (allDevices && userId != null) {
            revokeAllTokens(Long.valueOf(userId), "user_logout_all_devices");
        }

        log.info("登出完成: userId={}, jti={}, allDevices={}", userId, jti, allDevices);
    }

    @Override
    public void revokeAllTokens(Long userId, String reason) {
        if (userId == null) {
            return;
        }
        // TTL 由 jwt.expiration 推导而不是写死常量：写死会短于 token 实际寿命，
        // 下线记录先于 token 过期消失，旧 token 反而复活
        redis.opsForValue().set(KICKED_KEY + userId,
                String.valueOf(System.currentTimeMillis() / 1000),
                jwtUtil.maxTokenLifetime());
        log.info("强制下线: userId={}, reason={}", userId, reason);
    }

    @Override
    public boolean isRevoked(String jti, Long userId, long issuedAtEpochSecond) {
        // 两个键一次 MGET，保持单请求一次 Redis 往返
        List<String> values = redis.opsForValue().multiGet(List.of(
                BLACKLIST_KEY + jti,
                KICKED_KEY + userId
        ));
        if (values == null) {
            return false;
        }
        if (values.get(0) != null) {
            return true;
        }
        String kickedAt = values.get(1);
        return kickedAt != null && issuedAtEpochSecond < Long.parseLong(kickedAt);
    }
}
