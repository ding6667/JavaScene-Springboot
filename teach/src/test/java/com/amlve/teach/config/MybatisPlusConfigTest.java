package com.amlve.teach.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MybatisPlusConfig 回归测试：与公共模块同名拦截器 Bean 共存时容器可正常启动，
 * 且分页拦截器被补充到既有 Bean 上；精简上下文缺定义时兜底注册。
 * <p>
 * 背景：原实现声明同名 @Bean 与 bootstrap MyBatisConfig 冲突，启动抛
 * BeanDefinitionOverrideException（未开启 allow-bean-definition-overriding）。
 */
class MybatisPlusConfigTest {

    /**
     * 复刻 bootstrap MyBatisConfig：注册名为 mybatisPlusInterceptor 的空拦截器，
     * 模拟生产环境同名 Bean 已存在的冲突场景。
     */
    @Configuration(proxyBeanMethods = false)
    static class ExistingInterceptorConfig {

        @Bean
        public MybatisPlusInterceptor mybatisPlusInterceptor() {
            return new MybatisPlusInterceptor();
        }
    }

    @Test
    void context_shouldStartWithSingleInterceptor_whenSameNameBeanAlreadyExists() {
        // 回归：与 bootstrap 同名 Bean 共存不再冲突，拦截器上被补充分页能力
        new ApplicationContextRunner()
                .withUserConfiguration(ExistingInterceptorConfig.class, MybatisPlusConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                    MybatisPlusInterceptor interceptor = context.getBean(MybatisPlusInterceptor.class);
                    assertThat(interceptor.getInterceptors())
                            .hasSize(1)
                            .first()
                            .isInstanceOf(PaginationInnerInterceptor.class);
                });
    }

    @Test
    void context_shouldRegisterInterceptor_whenNoInterceptorBeanDefined() {
        // 精简上下文（如集成测试 SlimApp，不加载 bootstrap）：无拦截器定义时兜底注册并补充分页
        new ApplicationContextRunner()
                .withUserConfiguration(MybatisPlusConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context.getBean(MybatisPlusInterceptor.class).getInterceptors())
                            .hasSize(1)
                            .first()
                            .isInstanceOf(PaginationInnerInterceptor.class);
                });
    }

    @Test
    void enhancer_shouldNotDuplicate_whenPaginationAlreadyPresent() {
        // 公共层（bootstrap）将来补齐分页拦截器后：增强器跳过，不重复追加
        MybatisPlusInterceptor existing = new MybatisPlusInterceptor();
        existing.addInnerInterceptor(new PaginationInnerInterceptor());

        MybatisPlusConfig.PaginationInterceptorEnhancer enhancer =
                new MybatisPlusConfig.PaginationInterceptorEnhancer();
        enhancer.postProcessAfterInitialization(existing, "mybatisPlusInterceptor");

        assertThat(existing.getInterceptors()).hasSize(1);
    }

    @Test
    void enhancer_shouldIgnoreUnrelatedBeans() {
        // 非 MybatisPlusInterceptor 类型的 Bean 原样返回，不做任何处理
        Object bean = new Object();

        MybatisPlusConfig.PaginationInterceptorEnhancer enhancer =
                new MybatisPlusConfig.PaginationInterceptorEnhancer();
        Object processed = enhancer.postProcessAfterInitialization(bean, "otherBean");

        assertThat(processed).isSameAs(bean);
    }
}
