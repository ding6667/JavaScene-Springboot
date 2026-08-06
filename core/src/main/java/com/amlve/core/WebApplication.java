package com.amlve.core;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.amlve")
@ComponentScan(basePackages = "com.amlve")
@MapperScan(basePackages = {
        "com.amlve.system.mapper",
        "com.amlve.file.mapper",
        "com.amlve.ai.mapper"
})
@Slf4j
@EnableAsync
public class WebApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);

    }

}
