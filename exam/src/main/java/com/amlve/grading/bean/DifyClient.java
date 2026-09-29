package com.amlve.grading.bean;

import com.amlve.grading.properties.DifyProperty;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Dify 工作流客户端
 */
@Service("gradingDifyClient")
@Slf4j
@RequiredArgsConstructor
public class DifyClient {

    private final DifyProperty difyProperty;

    // 初始化 Spring 官方的 WebClient
    private WebClient webClient;
    @PostConstruct
    public void initialize() {
        this.webClient = WebClient.create(difyProperty.getBaseUrl());
    }


    public String runWorkflowBlocking(String apiKey, Map<String, Object> extraInputs) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", extraInputs != null ? extraInputs : new HashMap<>());
        requestBody.put("user", "test-user");
        requestBody.put("response_mode", "blocking");   // 改为 blocking

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
                                    return Mono.error(new RuntimeException("请求参数错误: " + errorBody));
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Dify 服务端错误: status={}, body={}",
                                            response.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("Dify 服务暂时不可用: " + errorBody));
                                })
                )
                .onStatus(s -> !s.is2xxSuccessful(), resp ->
                        resp.bodyToMono(String.class).flatMap(body -> {
                            log.error("错误响应: {}", body);
                            return Mono.error(new RuntimeException(body));
                        })
                )
                .bodyToMono(String.class)
                .block();
    }
    /**
     * 流式调用工作流 (原生 HTTP 实现)
     *
     * @param apiKey      动态传入的当前工作流 API 密钥
     * @param extraInputs 变量参数 Map (没有参数传 new HashMap<>() 或 null)
     * @return Flux<String> 纯净的 SSE 流式响应字符串
     */
    public Flux<String> streamWorkflow(String apiKey, Map<String, Object> extraInputs) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", extraInputs != null ? extraInputs : new HashMap<>());
        requestBody.put("user", "user-" + System.currentTimeMillis());
        requestBody.put("response_mode", "streaming"); // 关键：声明采用流式接收

        return webClient.post()
                .uri("/workflows/run")
                // 动态塞入方法参数里的 apiKey，实现随时切换工作流
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Dify 客户端错误: status={}, body={}",
                                            response.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("请求参数错误: " + errorBody));
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Dify 服务端错误: status={}, body={}",
                                            response.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("Dify 服务暂时不可用: " + errorBody));
                                })
                )
                .bodyToFlux(String.class)
                .onErrorResume(e -> {
                    log.error("原生 HTTP 流式请求发生崩溃", e);
                    return Flux.just("{\"error\": \"" + e.getMessage() + "\"}");
                });
    }


    /**
     * 非阻塞调用工作流，等待流全部吐完，返回合并后的完整结果
     *
     * @param apiKey      动态传入的当前工作流 API 密钥
     * @param extraInputs 变量参数 Map
     * @return 完整拼接后的字符串（包含 Dify 吐出来的所有块）
     */
    public String runWorkflow(String apiKey, Map<String, Object> extraInputs) {
        Flux<String> flux = streamWorkflow(apiKey, extraInputs);
        // 阻塞等待流式消息集结完毕，并用换行符连接
        return flux.collectList().block().stream()
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }
}
