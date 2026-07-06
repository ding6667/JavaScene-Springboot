package com.amlve.core;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = "com.amlve")
@ComponentScan(basePackages = "com.amlve")
@MapperScan(basePackages = "com.amlve.*.Mapper")
@Slf4j
public class WebApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);

    }

}
