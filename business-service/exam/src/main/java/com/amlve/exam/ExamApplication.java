package com.amlve.exam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = {
        "com.amlve.knowledgeapi.service",
        "com.amlve.agentquestionapi.service",
        "com.amlve.agentscoreapi.service",
        "com.amlve.teachapi.service",
        "com.amlve.systemapi.service",
        "com.amlve.studyapi.service"})
public class ExamApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamApplication.class, args);
    }
}
