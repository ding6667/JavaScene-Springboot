package com.amlve.teach.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * teach 模块自己的 Mapper 扫描配置。
 * 因 bootstrap 的 @MapperScan 未包含本模块包路径，在此模块内自行声明，无需改动公共代码。
 */
@Configuration
@MapperScan(basePackages = "com.amlve.teach.mapper")
public class TeachMapperConfig {
}
