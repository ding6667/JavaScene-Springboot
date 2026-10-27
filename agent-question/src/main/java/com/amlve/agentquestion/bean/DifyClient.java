package com.amlve.agentquestion.bean;

import com.amlve.common.constant.ErrorMessageConstant;
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
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Dify 出题工作流客户端。
 * base-url 从 application.yml 读取，工作流应用密钥每次调用动态传入。
 * 显式命名 questionDifyClient，避免与 exam 模块（com.amlve.grading 包）的同名 DifyClient 默认 bean 名冲突。
 */
@Service("questionDifyClient")
@Slf4j
public class DifyClient {

    @Value("${dify.base-url:https://api.dify.ai/v1}")
    private String baseUrl;

    @Value("${dify.read-timeout:300}")
    private long readTimeout;

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
     * 阻塞调用出题工作流，返回 Dify 原始 JSON 响应
     */
    public String runWorkflowBlocking(String apiKey, Map<String, Object> extraInputs) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", extraInputs != null ? extraInputs : new HashMap<>());
        requestBody.put("user", "teacher-question");
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
}