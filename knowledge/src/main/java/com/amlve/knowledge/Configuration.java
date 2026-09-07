package com.amlve.knowledge;

import lombok.Data;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;

/**
 * knowledge 模块配置
 * <p>
 * 说明：与 Dify 知识库通信的 WebClient 已由 agent-qa 模块的
 * {@code com.amlve.agentqa.config.DifyClientConfig#difyWebClient()} 统一封装
 * （含 datasetApiKey 认证头），本模块不重复创建同名 Bean，直接通过
 * {@code QAAgentClient} 接口调用；本配置类负责 Mapper 扫描与知识库 ID 配置。
 * <p>
 * 注意：类名按开发计划定为 Configuration，与 Spring 的 @Configuration 注解同名，
 * 因此注解使用全限定名而不能 import。
 */
@Data
@org.springframework.context.annotation.Configuration
@MapperScan("com.amlve.knowledge.mapper")
public class Configuration {

    /**
     * 默认知识库ID（Dify dataset_id），从 application-dev.yml 读取：
     * <pre>
     * dify:
     *   dataset-id: a8e0e5b5-78c6-4130-a5ce-25feb0e0b4ac
     * </pre>
     */
    @Value("${dify.dataset-id:}")
    private String difyDatasetId;
}
