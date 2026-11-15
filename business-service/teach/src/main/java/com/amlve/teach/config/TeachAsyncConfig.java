package com.amlve.teach.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 备课模块异步生成线程池配置。
 */
@Configuration
public class TeachAsyncConfig {

    @Bean(name = "prepareExecutor")
    public Executor prepareExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
