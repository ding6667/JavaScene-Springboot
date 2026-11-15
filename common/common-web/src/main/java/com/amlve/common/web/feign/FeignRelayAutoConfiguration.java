package com.amlve.common.web.feign;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 只要类路径上有 Feign 就注册身份透传拦截器，业务代码无感。
 * 解码器不放在这里：它必须通过 @FeignClient(configuration = FeignConfig.class) 逐个客户端生效。
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignRelayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(UserHeaderRelayInterceptor.class)
    public RequestInterceptor userHeaderRelayInterceptor() {
        return new UserHeaderRelayInterceptor();
    }
}
