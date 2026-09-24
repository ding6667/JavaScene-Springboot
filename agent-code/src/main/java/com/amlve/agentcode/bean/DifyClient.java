package com.amlve.agentcode.bean;

import com.amlve.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Dify 工作流客户端：
 * 工作流应用 API 密钥（app- 开头）逐次调用动态传入，便于切换不同工作流应用
 */
@Service
@Slf4j
public class DifyClient {

    @Value("${dify.base-url:https://api.dify.ai/v1}")
    private String baseUrl;

    private WebClient webClient;

    @PostConstruct
    public void initialize() {
        this.webClient = WebClient.create(baseUrl);
    }

    /**
     * 阻塞调用工作流，等待执行完毕，返回 Dify 原始 JSON 响应
     *
     * @param apiKey      动态传入的工作流应用 API 密钥
     * @param extraInputs 工作流 inputs 变量 Map（无参数传 null 或空 Map）
     * @return 响应 JSON（输出在 data.outputs 中）
     */
    public String runWorkflowBlocking(String apiKey, Map<String, Object> extraInputs) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", extraInputs != null ? extraInputs : new HashMap<>());
        requestBody.put("user", "test-user");
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
                                    return Mono.error(new BusinessException("Dify 工作流请求参数错误: " + errorBody));
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
     * 流式调用工作流，返回 SSE 流式响应字符串流
     *
     * @param apiKey      动态传入的工作流应用 API 密钥
     * @param extraInputs 工作流 inputs 变量 Map
     * @return Flux&lt;String&gt; 纯净的 SSE 流式响应字符串
     */
    public Flux<String> streamWorkflow(String apiKey, Map<String, Object> extraInputs) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", extraInputs != null ? extraInputs : new HashMap<>());
        requestBody.put("user", "user-" + System.currentTimeMillis());
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
                                    return Mono.error(new BusinessException("Dify 工作流请求参数错误: " + errorBody));
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
                    log.error("原生 HTTP 流式请求发生崩溃", e);
                    return Flux.just("{\"error\": \"" + e.getMessage() + "\"}");
                });
    }
}
