package com.amlve.common.web.feign;

import com.amlve.common.constant.AuthHeaderConstant;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 跨服务调用时把当前请求的身份继续传下去：网关给进来的那几个头，
 * 以及原始 Authorization（便于链路末端仍能吊销/刷新 token）。
 * 没有请求上下文（@Async、定时任务）时静默跳过，此时走 /internal 的免鉴权通道。
 */
public class UserHeaderRelayInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION = "Authorization";

    @Override
    public void apply(feign.RequestTemplate template) {
        HttpServletRequest current = currentRequest();
        if (current == null) {
            return;
        }
        relay(template, current, AUTHORIZATION);
        relay(template, current, AuthHeaderConstant.USER_ID);
        relay(template, current, AuthHeaderConstant.USER_NAME);
        relay(template, current, AuthHeaderConstant.USER_ROLES);
    }

    private void relay(feign.RequestTemplate template, HttpServletRequest current, String header) {
        String value = current.getHeader(header);
        if (value != null && !value.isBlank()) {
            template.header(header, value);
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
