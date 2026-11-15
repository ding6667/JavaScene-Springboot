package com.amlve.common.dify;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.exception.BusinessException;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Dify 工作流客户端。base-url 与超时来自配置，工作流应用密钥（app- 开头）逐次传入，
 * 便于同一进程调用不同的 Dify 应用。流式只给结构化 SSE 事件一种，
 * 因为唯一调用方（答疑）按事件名解析 text_chunk / workflow_finished。
 */
@Slf4j
public class DifyWorkflowClient {

    private static final String WORKFLOW_RUN_URI = "/workflows/run";
    private static final int MAX_IN_MEMORY = 16 * 1024 * 1024;

    private final WebClient webClient;
    private final String user;

    public DifyWorkflowClient(String baseUrl, long readTimeoutSeconds, long connectTimeoutSeconds, String user) {
        this.user = user;
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Duration.ofSeconds(connectTimeoutSeconds).toMillis());
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY))
                .build();
    }

    /**
     * 阻塞调用工作流，返回 Dify 原始 JSON 响应（业务输出在 data.outputs 里）
     *
     * @param apiKey      工作流应用 API 密钥
     * @param extraInputs 工作流 inputs 变量，可为 null
     */
    public String runWorkflowBlocking(String apiKey, Map<String, Object> extraInputs) {
        return request(apiKey, extraInputs, "blocking", user)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::logAndReject)
                .bodyToMono(String.class)
                .block();
    }

    /**
     * 流式调用工作流，返回结构化 SSE 事件（event 为事件名，data 为 JSON 字符串），
     * 事件语义由调用方解析；传输层异常抛 {@link BusinessException}
     */
    public Flux<ServerSentEvent<String>> streamWorkflowEvents(String apiKey, Map<String, Object> extraInputs) {
        return request(apiKey, extraInputs, "streaming", user + "-" + System.currentTimeMillis())
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::logAndReject)
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                .onErrorMap(e -> !(e instanceof BusinessException),
                        e -> new BusinessException(ErrorMessageConstant.AGENT_UNAVAILABLE, e));
    }

    private WebClient.RequestHeadersSpec<?> request(String apiKey, Map<String, Object> extraInputs,
                                                    String responseMode, String endUser) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", extraInputs != null ? extraInputs : new HashMap<>());
        requestBody.put("user", endUser);
        requestBody.put("response_mode", responseMode);

        return webClient.post()
                .uri(WORKFLOW_RUN_URI)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody);
    }

    /**
     * 状态码和原始响应体只进日志，对外统一回「智能体不可用」，
     * 原始响应体属于排障信息，不能透传给学生
     */
    private Mono<BusinessException> logAndReject(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(errorBody -> {
                    log.error("Dify 调用失败: status={}, body={}", response.statusCode(), errorBody);
                    return Mono.just(new BusinessException(ErrorMessageConstant.AGENT_UNAVAILABLE));
                });
    }
}
