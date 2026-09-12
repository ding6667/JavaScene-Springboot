package com.amlve.agentprepare.bean;

import com.amlve.common.exception.BusinessException;
import io.netty.channel.ChannelOption;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Dify 备课工作流客户端。
 * base-url 从 application.yml 读取；工作流应用密钥（app- 开头）逐次调用动态传入，便于切换工作流应用。
 * 备课生成耗时较长，读取超时复用 dify.read-timeout（默认 300 秒）。
 * 显式命名 prepareDifyClient：避免与 exam 模块（com.amlve.grading 包）的同名 DifyClient 默认 bean 名冲突。
 */
@Service("prepareDifyClient")
@Slf4j
public class DifyClient {

    @Value("${dify.base-url:https://api.dify.ai/v1}")
    private String baseUrl;

    /** 读取超时（秒），备课工作流 LLM 生成耗时较长，默认 300 */
    @Value("${dify.read-timeout:300}")
    private long readTimeout;

    /** 连接超时（秒） */
    @Value("${dify.connect-timeout:10}")
    private long connectTimeout;

    private WebClient webClient;

    @PostConstruct
    public void initialize() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(readTimeout))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Duration.ofSeconds(connectTimeout).toMillis());
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    /**
     * 阻塞调用工作流，等待执行完毕，返回 Dify 原始 JSON 响应
     *
     * @param apiKey      动态传入的工作流应用 API 密钥
     * @param extraInputs 工作流 inputs 变量 Map（无参数传 null 或空 Map）
     * @return 响应 JSON（备课五字段在 data.outputs 中）
     */
    public String runWorkflowBlocking(String apiKey, Map<String, Object> extraInputs) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", extraInputs != null ? extraInputs : new HashMap<>());
        requestBody.put("user", "teacher-prepare");
        requestBody.put("response_mode", "blocking");

        return webClient.post()
                .uri("/workflows/run")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Dify 客户端错误: status={}, body={}",
                                            response.statusCode(), errorBody);
                                    return Mono.error(new BusinessException("Dify 备课工作流请求参数错误: " + errorBody));
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Dify 服务端错误: status={}, body={}",
                                            response.statusCode(), errorBody);
                                    return Mono.error(new BusinessException("Dify 服务暂时不可用: " + errorBody));
                                })
                )
                .bodyToMono(String.class)
                .block();
    }

    /**
     * 流式调用工作流，返回 SSE 流式响应字符串流（预留，备课当前使用 blocking 模式）
     *
     * @param apiKey      动态传入的工作流应用 API 密钥
     * @param extraInputs 工作流 inputs 变量 Map
     * @return Flux&lt;String&gt; 纯净的 SSE 流式响应字符串
     */
    public Flux<String> streamWorkflow(String apiKey, Map<String, Object> extraInputs) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", extraInputs != null ? extraInputs : new HashMap<>());
        requestBody.put("user", "teacher-prepare-" + System.currentTimeMillis());
        requestBody.put("response_mode", "streaming");

        return webClient.post()
                .uri("/workflows/run")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Dify 客户端错误: status={}, body={}",
                                            response.statusCode(), errorBody);
                                    return Mono.error(new BusinessException("Dify 备课工作流请求参数错误: " + errorBody));
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Dify 服务端错误: status={}, body={}",
                                            response.statusCode(), errorBody);
                                    return Mono.error(new BusinessException("Dify 服务暂时不可用: " + errorBody));
                                })
                )
                .bodyToFlux(String.class)
                .onErrorResume(e -> {
                    log.error("Dify 流式请求发生异常", e);
                    return Flux.just("{\"error\": \"" + e.getMessage() + "\"}");
                });
    }
}
