package com.amlve.agentqa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = {"com.amlve.knowledgeapi.service"})
public class AgentQaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentQaApplication.class, args);
    }
}
