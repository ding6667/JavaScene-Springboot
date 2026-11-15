package com.amlve.common.dify;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 只有配了 dify.base-url 的服务才装配工作流客户端；user 用各服务自己的标识，
 * 保持迁移前后在 Dify 侧看到的调用来源一致。
 */
@AutoConfiguration
@ConditionalOnProperty("dify.base-url")
public class DifyWorkflowAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DifyWorkflowClient difyWorkflowClient(
            @Value("${dify.base-url}") String baseUrl,
            @Value("${dify.read-timeout:300}") long readTimeout,
            @Value("${dify.connect-timeout:10}") long connectTimeout,
            @Value("${dify.user:javascene}") String user) {
        return new DifyWorkflowClient(baseUrl, readTimeout, connectTimeout, user);
    }
}
