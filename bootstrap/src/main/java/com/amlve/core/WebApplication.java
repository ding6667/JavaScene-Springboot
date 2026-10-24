package com.amlve.core;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication(scanBasePackages = "com.amlve")
@ComponentScan(basePackages = "com.amlve")
@MapperScan(basePackages = {
        "com.amlve.system.mapper",
        "com.amlve.dialogue.mapper"
})
@Slf4j
@EnableAsync
@EnableMethodSecurity(prePostEnabled = true)
public class WebApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);

    }

}
