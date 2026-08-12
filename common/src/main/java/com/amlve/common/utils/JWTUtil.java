package com.amlve.common.utils;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JWTUtil {
    // 配置文件填写密钥，长度至少32位！
    @Value("${jwt.secret:abcdefghabcdefghabcdefgh12345678}")
    private String secretStr;

    // token有效期，单位毫秒  2小时
    private static final long EXPIRE_TIME = 2 * 60 * 60 * 1000;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] bytes = secretStr.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        secretKey = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * 生成Token
     * @param userId 用户唯一标识
     * @return token字符串
     */
    public String generateToken(Long userId) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + EXPIRE_TIME);

        return Jwts.builder()
                // 载荷：存入userId（不要放敏感密码）
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expireDate)
                // 签名加密
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析token，获取userId
     */
    public Long getUserIdByToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 校验token是否合法、没过期
     */
    public boolean verifyToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCodeConstant.UNAUTHORIZED, ErrorMessageConstant.TOKEN_EXPIRED);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeConstant.UNAUTHORIZED, ErrorMessageConstant.TOKEN_INVALID);
        }
    }
}
