package com.amlve.teach.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * teach 模块 MyBatis-Plus 分页配置（适配公共模块已有 Bean，避免同名冲突）。
 * <p>
 * bootstrap 的 MyBatisConfig 已注册名为 mybatisPlusInterceptor 的 Bean（空拦截器），
 * 本模块若再声明同名 @Bean 会因 Bean 定义重复导致应用启动失败（工程未开启
 * allow-bean-definition-overriding）。因此本类不再注册同名 Bean，改用两个后置处理器：
 * <ol>
 *     <li>定义兜底：容器缺少拦截器 Bean 定义时补注册一个空拦截器（精简上下文/单模块启动场景，
 *     如集成测试 SlimApp 不扫描 bootstrap），保证 selectPage 行为与生产一致；</li>
 *     <li>实例增强：拦截器 Bean 初始化后补充 PaginationInnerInterceptor。selectPage 物理分页
 *     依赖该拦截器，缺失时分页退化为全量查询（total 正确、records 不截断）。</li>
 * </ol>
 * 若未来公共层自行补齐分页拦截器（协作方案落地），增强器检测到已存在分页拦截器会自动跳过，不重复叠加。
 * <p>
 * 两个后置处理器均声明为 static @Bean：后置处理器需尽早注册，避免宿主配置类被容器过早实例化。
 */
@Configuration
public class MybatisPlusConfig {

    /** 容器中拦截器 Bean 名（bootstrap MyBatisConfig 的 @Bean 方法名） */
    static final String INTERCEPTOR_BEAN_NAME = "mybatisPlusInterceptor";

    @Bean
    public static InterceptorDefinitionRegistrar interceptorDefinitionRegistrar() {
        return new InterceptorDefinitionRegistrar();
    }

    @Bean
    public static PaginationInterceptorEnhancer paginationInterceptorEnhancer() {
        return new PaginationInterceptorEnhancer();
    }

    /**
     * 定义兜底：容器缺拦截器 Bean 定义时补一个空拦截器定义。
     * <p>
     * BeanDefinitionRegistryPostProcessor 晚于所有配置类的 Bean 定义注册执行，
     * 判断时 bootstrap/本模块的定义均已就绪：生产环境 bootstrap 已注册同名 Bean，
     * 此处不动作；精简上下文（不加载 bootstrap）则兜底注册，分页能力不缺失。
     */
    static class InterceptorDefinitionRegistrar implements BeanDefinitionRegistryPostProcessor {

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            if (!registry.containsBeanDefinition(INTERCEPTOR_BEAN_NAME)) {
                BeanDefinition definition = BeanDefinitionBuilder
                        .genericBeanDefinition(MybatisPlusInterceptor.class)
                        .getBeanDefinition();
                registry.registerBeanDefinition(INTERCEPTOR_BEAN_NAME, definition);
            }
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            // 无需处理
        }
    }

    /**
     * 实例增强：拦截器 Bean 初始化后追加分页内部拦截器。
     * 已含分页拦截器时跳过（防重复叠加）；分页拦截器按官方建议置于拦截器链末尾
     * （addInnerInterceptor 为尾部追加）。
     */
    static class PaginationInterceptorEnhancer implements BeanPostProcessor {

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof MybatisPlusInterceptor interceptor
                    && interceptor.getInterceptors().stream().noneMatch(PaginationInnerInterceptor.class::isInstance)) {
                interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
            }
            return bean;
        }
    }
}
