package com.amlve.common.mybatis.config;

import com.amlve.common.mybatis.handler.MyBatisPlusMetaHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

/**
 * MyBatis-Plus 分页插件 + 审计字段自动填充，业务服务不必各写一份。
 */
@AutoConfiguration
@ConditionalOnClass(MybatisPlusInterceptor.class)
@Import({MyBatisConfig.class, MyBatisPlusMetaHandler.class})
public class MybatisInfraAutoConfiguration {
}
