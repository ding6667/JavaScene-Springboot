package com.amlve.agentprepare;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 完整上下文加载测试。
 * agent-prepare 是纯客户端模块，无独立启动配置与数据源，
 * 单模块 @SpringBootTest 无法初始化上下文，暂时禁用。
 * 业务逻辑测试见 PrepareClientTest。
 */
@Disabled("agent-prepare 模块无独立启动配置，完整上下文测试需在 bootstrap 聚合环境运行")
@SpringBootTest
class AgentPrepareApplicationTests {

	@Test
	void contextLoads() {
	}

}
