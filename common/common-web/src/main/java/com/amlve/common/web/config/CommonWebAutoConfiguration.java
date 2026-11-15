package com.amlve.common.web.config;

import com.amlve.common.web.handler.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

/**
 * 业务服务（Servlet 栈）的公共 Web 设施：全局异常、Jackson、Redis 序列化、CORS/MVC。
 * 网关是 Reactive，不会装配这些类。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({JacksonConfig.class, RedisConfig.class, WebMVCConfig.class, GlobalExceptionHandler.class})
public class CommonWebAutoConfiguration {
}
