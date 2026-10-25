package com.amlve.common.utils;

import com.amlve.common.exception.BusinessException;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.constant.ErrorMessageConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class JWTUtil {

    @Value("${jwt.secret}")
    private String secretStr;

    @Value("${jwt.expiration}") // 毫秒，例如 7200000
    private Long expiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] bytes = secretStr.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret 长度不足 32 字节，HS256 无法工作");
        }
        secretKey = Keys.hmacShaKeyFor(bytes);
        log.info("JWT 密钥初始化完成，长度 = {} 字节", bytes.length);
    }

    /**
     * 生成 JWT：userId + username + roles + jti
     */
    public String generateToken(Long userId, String username, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("roles", roles == null ? Collections.emptyList() : roles);

        Date now = new Date();
        String token = Jwts.builder()
                .setClaims(claims)              // setClaims 必须放最前，它会覆盖其他
                .setSubject(String.valueOf(userId))
                .setId(UUID.randomUUID().toString())   // jti
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration))
                .signWith(secretKey)
                .compact();

        // 自验：失败直接抛，绝不把坏 token 发出去
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
        } catch (Exception e) {
            log.error("JWT 自验失败，密钥或配置有误：{}", e.getMessage());
            throw new IllegalStateException("JWT 签发异常", e);
        }
        return token;
    }

    /**
     * 验签并返回 claims。调用方一次取齐 subject / jti / iat，避免同一请求重复验签。
     */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验签但忽略过期：登出一个已经放着过夜的 token 时仍要拿到 jti 才能拉黑。
     * 只放过 ExpiredJwtException，签名照样校验——绕过验签会让伪造 token 也能写黑名单。
     */
    public Claims parseIgnoringExpiration(String token) {
        try {
            return parseClaims(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    /**
     * token 最大存活时间。吊销记录的 TTL 由它推导，避免与 jwt.expiration 配置脱节
     */
    public Duration maxTokenLifetime() {
        return Duration.ofMillis(expiration);
    }

    /** 校验签名 + 过期 */
    public boolean verifyToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCodeConstant.UNAUTHORIZED,
                    ErrorMessageConstant.TOKEN_EXPIRED);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeConstant.UNAUTHORIZED,
                    ErrorMessageConstant.TOKEN_INVALID);
        }
    }

    public String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}