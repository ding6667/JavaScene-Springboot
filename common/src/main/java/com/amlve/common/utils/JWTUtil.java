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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JWTUtil {

    // 配置文件填写密钥，长度至少32位！
    @Value("${jwt.secret}")
    private String secretStr;

    @Value("${jwt.expiration}") // 默认2小时
    private Long expiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] bytes = secretStr.getBytes(StandardCharsets.UTF_8);
        secretKey = Keys.hmacShaKeyFor(bytes);
        log.info("========== JWT 密钥初始化 ==========");
        log.info("  secretStr = [{}]", secretStr);
        log.info("  secretStr 长度 = {} 字节 / {} 字符", bytes.length, secretStr.length());
        log.info("====================================");
    }

    /**
     * 生成 JWT，同时存储 userId 和 username
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();

        // 自验：如果这里都过不了，说明密钥初始化就有问题
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            log.info("✅ generateToken 自验通过 | userId={} | 生成的token前50={}", userId, token.substring(0, Math.min(50, token.length())));
        } catch (Exception e) {
            log.error("❌ generateToken 自验失败！！！原因：{}", e.getMessage());
        }
        return token;
    }

    /**
     * 解析 token，获取 userId
     */
    public Long getUserIdByToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Long.class);
    }

    /**
     * 解析 token，获取 username
     */
    public String getUsernameByToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("username", String.class);
    }

    /**
     * 校验 token 是否合法、没过期
     */
    public boolean verifyToken(String token) {
        // 诊断：打印 Filter 收到的 token
        if (token != null) {
            log.info("🔍 verifyToken 收到的token | 长度={} | 前50={}", token.length(), token.substring(0, Math.min(50, token.length())));
        }
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期: {}", e.getMessage());
            throw new BusinessException(ErrorCodeConstant.UNAUTHORIZED, ErrorMessageConstant.TOKEN_EXPIRED);
        } catch (Exception e) {
            log.warn("Token 无效: {}", e.getMessage());
            throw new BusinessException(ErrorCodeConstant.UNAUTHORIZED, ErrorMessageConstant.TOKEN_INVALID);
        }
    }

    /**
     * 从请求头中提取 Token（工具方法，方便调用）
     */
    public String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}