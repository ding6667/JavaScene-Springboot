package com.amlve.core.configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 全局插件配置
 * <p>
 * {@link MybatisPlusInterceptor} 只是外壳，真正改写 SQL 的是注册在其中的 inner 拦截器。
 * 分页必须由 {@link PaginationInnerInterceptor} 承担：它在执行前生成 COUNT 语句回填 total，
 * 并给原 SQL 拼 LIMIT。缺了 inner 时 selectPage 会退化成"查全表 + total 恒为 0"，
 * 且不会报任何错，故新增分页接口时务必确认此处的 inner 已注册。
 * <p>
 * 逻辑删除（@TableLogic）为 MyBatis-Plus 内置能力，不依赖此处注册。
 */
@Configuration
public class MyBatisConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 兜底单页上限：防止前端或调用方传入超大 pageSize 把整表拖走
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
