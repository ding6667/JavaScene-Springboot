package com.amlve.exam.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * exam 子域 Mapper 扫描配置。
 * <p>
 * bootstrap 的 @MapperScan 未包含本模块包路径，在模块内自行声明，无需改动公共代码。
 */
@Configuration
@MapperScan(basePackages = "com.amlve.exam.mapper")
public class ExamMapperConfig {
}