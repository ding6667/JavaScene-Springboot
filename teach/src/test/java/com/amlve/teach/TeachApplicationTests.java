package com.amlve.teach;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 完整上下文加载测试。
 * teach 模块自身不包含数据源配置（全局配置在 bootstrap 的 application.yml），
 * 单模块运行 @SpringBootTest 无法初始化 MyBatis-Plus 数据源，暂时禁用。
 * 业务逻辑测试见 TeachServiceImplTest / HomeworkControllerTest / PrepareControllerTest。
 */
@Disabled("teach 模块无独立数据源配置，完整上下文测试需在 bootstrap 聚合环境运行")
@SpringBootTest
class TeachApplicationTests {

    @Test
    void contextLoads() {
    }

}
