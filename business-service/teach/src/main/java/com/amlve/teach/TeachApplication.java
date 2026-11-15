package com.amlve.teach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
// 只扫外部消费的包：teach 自己提供 teach-api，扫进来会和本地 TeachServiceImpl 撞成两个候选
@EnableFeignClients(basePackages = {
        "com.amlve.examapi.service",
        "com.amlve.knowledgeapi.service",
        "com.amlve.systemapi.service",
        "com.amlve.agentprepareapi.service"})
public class TeachApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeachApplication.class, args);
    }
}
