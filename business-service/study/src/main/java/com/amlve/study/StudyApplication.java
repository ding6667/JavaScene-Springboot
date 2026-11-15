package com.amlve.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = {
        "com.amlve.agentcodeapi.service",
        "com.amlve.agentscoreapi.service",
        "com.amlve.examapi.service",
        "com.amlve.knowledgeapi.service"})
public class StudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyApplication.class, args);
    }
}
