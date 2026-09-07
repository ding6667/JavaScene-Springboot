package com.amlve.agentqa.config;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Dify 知识库通信配置
 * <p>
 * datasetApiKey 与 base-url 从 application.yml 读取：
 * <pre>
 * dify:
 *   base-url: http://your-dify-host/v1      # Dify 服务 API 端点（知识库页右上角"服务API"中可查看）
 *   dataset-api-key: dataset-xxxxxxxx       # 知识库 API 密钥
 *   dataset-id: xxxxxxxx-...                # 默认知识库ID（knowledge 模块读取）
 *   read-timeout: 120                       # 读取超时（秒）
 *   connect-timeout: 10                     # 连接超时（秒）
 * </pre>
 */
@Slf4j
@Configuration
public class DifyClientConfig {

    @Value("${dify.dataset-api-key:}")
    private String datasetApiKey;

    @Value("${dify.base-url:https://api.dify.ai/v1}")
    private String baseUrl;

    /** 读取超时（秒），检索/上传大文档可能较慢，配置默认120 */
    @Value("${dify.read-timeout:120}")
    private long readTimeout;

    /** 连接超时（秒） */
    @Value("${dify.connect-timeout:10}")
    private long connectTimeout;

    @Bean
    public WebClient difyWebClient() {
        if (datasetApiKey == null || datasetApiKey.isBlank()) {
            log.warn("dify.dataset-api-key 未配置，Dify 知识库接口将不可用，请在配置文件中配置");
        }
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(readTimeout))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Duration.ofSeconds(connectTimeout).toMillis());
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + datasetApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }
}
