package com.amlve.study.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.amlve.study.mapper")
public class StudyMapperConfig {
}
