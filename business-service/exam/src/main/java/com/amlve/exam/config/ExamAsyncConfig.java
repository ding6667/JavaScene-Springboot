package com.amlve.exam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 考评模块异步批改线程池配置。
 */
@Configuration
public class ExamAsyncConfig {

    @Bean(name = "examGradingExecutor")
    public Executor examGradingExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}