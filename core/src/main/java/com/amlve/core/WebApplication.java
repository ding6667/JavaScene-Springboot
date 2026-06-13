package com.amlve.core;

import com.amlve.ai.Service.DifyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = "com.amlve")
@ComponentScan(basePackages = "com.amlve")
@Slf4j
public class WebApplication {


    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);

    }

}
