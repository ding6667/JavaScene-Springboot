package com.amlve.common.security.config;

import com.amlve.common.security.token.TokenRevocationService;
import com.amlve.common.security.token.TokenRevocationServiceImpl;
import com.amlve.common.utils.JWTUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 签发与吊销相关的能力只在持有 jwt.secret 的服务里装配（当前是 system 服务）。
 * 其余服务靠网关转发的身份头，不需要密钥，也不会因为缺配置而起不来。
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty("jwt.secret")
public class SecurityTokenAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JWTUtil jwtUtil() {
        return new JWTUtil();
    }

    @Bean
    @ConditionalOnMissingBean(TokenRevocationService.class)
    public TokenRevocationService tokenRevocationService(JWTUtil jwtUtil, StringRedisTemplate redis) {
        return new TokenRevocationServiceImpl(jwtUtil, redis);
    }
}
