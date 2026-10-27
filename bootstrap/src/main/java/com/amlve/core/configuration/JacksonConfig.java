package com.amlve.core.configuration;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全局 Jackson 序列化配置。
 * <p>
 * 雪花 ID（Long）超过 JS 安全整数（2^53-1），前端 JS 解析时会导致精度丢失，
 * 因此将 Long 统一序列化为字符串下发给前端；前端回传字符串时 Spring 会自动反
 * 序列化为 Long，不影响后端接收。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder.serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}