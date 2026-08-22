package com.amlve.dialogue.utils;


import com.amlve.dialogue.entity.PendingOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingOperationCache {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "dialogue:pending:";
    private static final int TTL_MINUTES = 5;

    public void save(String operationId, PendingOperation operation) {
        String key = KEY_PREFIX + operationId;
        operation.setCreateTime(LocalDateTime.now());
        operation.setExpireTime(LocalDateTime.now().plus(TTL_MINUTES, ChronoUnit.MINUTES));
        redisTemplate.opsForValue().set(key, operation, TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("待确认操作已缓存到 Redis: operationId={}, tool={}, TTL={}分钟",
                operationId, operation.getToolName(), TTL_MINUTES);
    }

    public PendingOperation get(String operationId) {
        String key = KEY_PREFIX + operationId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof PendingOperation) {
            PendingOperation op = (PendingOperation) value;
            if (op.isExpired()) {
                redisTemplate.delete(key);
                log.debug("待确认操作已过期（Redis）: operationId={}", operationId);
                return null;
            }
            return op;
        }
        return null;
    }

    public void remove(String operationId) {
        String key = KEY_PREFIX + operationId;
        redisTemplate.delete(key);
        log.debug("待确认操作已从 Redis 删除: operationId={}", operationId);
    }

    public boolean exists(String operationId) {
        String key = KEY_PREFIX + operationId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 刷新 TTL（用户操作时延长过期时间）
     */
    public void refresh(String operationId) {
        String key = KEY_PREFIX + operationId;
        redisTemplate.expire(key, TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("已刷新 Redis TTL: operationId={}", operationId);
    }
}