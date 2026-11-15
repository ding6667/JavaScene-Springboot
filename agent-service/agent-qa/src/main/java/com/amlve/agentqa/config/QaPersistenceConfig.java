package com.amlve.agentqa.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * agent-qa 模块持久层配置
 * <p>
 * bootstrap 的 {@code @MapperScan} 只列了 system/file/grading/dialogue 四个包，
 * 本模块从任务 5 起才有自己的表（qa_record），故在本模块内声明自己的扫描包，
 * 不去改公共启动类的注解——那属于共享文件，改一次要全项目回归。
 */
@Configuration
@MapperScan("com.amlve.agentqa.mapper")
public class QaPersistenceConfig {
}
