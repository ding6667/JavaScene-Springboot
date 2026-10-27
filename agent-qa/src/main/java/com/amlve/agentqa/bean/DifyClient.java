package com.amlve.agentqa.bean;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Dify 工作流客户端（与知识库 DatasetWebClient 分离）：
 * 工作流应用 API 密钥（app- 开头）逐次调用动态传入，便于切换不同工作流应用
 * <p>
 * Bean 显式命名 qaDifyClient：避免与 exam 模块
 * {@code com.amlve.grading.bean.DifyClient} 的默认 Bean 名 difyClient 冲突
 * （多模块共容器时 Spring 默认 Bean 名只看类名不看包名）
 */
@Service("qaDifyClient")
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
     * @return 响应 JSON（答案在 data.outputs 中）
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
                                    return Mono.error(new BusinessException(ErrorMessageConstant.AGENT_UNAVAILABLE));
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Dify 服务端错误: status={}, body={}",
                                            response.statusCode(), errorBody);
                                    return Mono.error(new BusinessException(ErrorMessageConstant.AGENT_UNAVAILABLE));
                                })
                )
                .bodyToMono(String.class)
                .block();
    }

    /**
     * 流式调用工作流，返回结构化 SSE 事件流
     * <p>
     * Dify streaming 模式按事件推送：text_chunk（答案文本片段）、
     * workflow_finished（执行结束，含 status 与 outputs）、error 等，
     * 事件语义由上层 {@code QAAgentClientImpl} 解析。
     *
     * @param apiKey      动态传入的工作流应用 API 密钥
     * @param extraInputs 工作流 inputs 变量 Map
     * @return Dify SSE 事件流（event 为事件名，data 为 JSON 字符串）
     */
    public Flux<ServerSentEvent<String>> streamWorkflow(String apiKey, Map<String, Object> extraInputs) {
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
                                    return Mono.error(new BusinessException(ErrorMessageConstant.AGENT_UNAVAILABLE));
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Dify 服务端错误: status={}, body={}",
                                            response.statusCode(), errorBody);
                                    return Mono.error(new BusinessException(ErrorMessageConstant.AGENT_UNAVAILABLE));
                                })
                )
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .onErrorMap(e -> !(e instanceof BusinessException),
                        e -> new BusinessException(ErrorMessageConstant.AGENT_UNAVAILABLE, e));
    }
}
