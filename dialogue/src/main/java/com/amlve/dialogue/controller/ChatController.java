package com.amlve.dialogue.controller;

import com.amlve.common.utils.SecurityUtils;
import com.amlve.dialogue.entity.PendingOperation;
import com.amlve.dialogue.service.ToolService;
import com.amlve.dialogue.utils.PendingOperationCache;
import com.amlve.dialogue.request.ReconfirmRequest;
import com.amlve.dialogue.enumeration.Intent;
import com.amlve.dialogue.enumeration.TargetScope;
import com.amlve.dialogue.service.AiService;
import com.amlve.dialogue.service.RouterLlmService;
import com.amlve.dialogue.vo.ChatStreamResponse;
import com.amlve.systemapi.service.SettingsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequestMapping("/api/chat")
@RestController
@RequiredArgsConstructor
public class ChatController {

    @Value("${app.crypto.salt}")
    private String salt;

    @Value("${app.crypto.master-password}")
    private String masterPassword;

    private final RouterLlmService routerService;
    private final AiService aiService;
    private final SettingsService settingsService;
    private final PendingOperationCache pendingCache;
    private final ToolService toolService;
    private final ObjectMapper objectMapper;


    @GetMapping(value = "/streamChat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamResponse>> streamChat(
            @RequestParam("message") String message,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam(value = "confirm", defaultValue = "false") boolean confirm,
            @RequestParam(value = "operationId", required = false) String operationId) {
        log.info("streamChat: {}, {}, {}, {}", message, conversationId, confirm, operationId);
        String userApiKey = settingsService.getEncryptedAPIKey(SecurityUtils.getUserId());
        // ========== 分支 1：确认后的执行流程 ==========
        if (confirm && operationId != null) {
            PendingOperation pending = pendingCache.get(operationId);
            if (pending == null) {
                return Flux.just(errorEvent("操作已过期或不存在，请重新发起"));
            }

            // 执行工具调用
            String toolResult = toolService.executeTool(pending.getToolName(), pending.getToolParams());

            // 构造让 LLM 格式化输出的消息
            String formattedPrompt = toolService.buildExecutePrompt(pending, toolResult);

            // 流式调用 LLM 生成最终执行结果反馈（不带工具）
            return getServerSentEventFlux(userApiKey, formattedPrompt, conversationId, List.of(), "");
        }

        // ========== 分支 2：正常首次请求 ==========
        // 解密 API Key
        TextEncryptor encryptor = Encryptors.text(masterPassword, salt);
        userApiKey = encryptor.decrypt(userApiKey);
        log.info("userApiKey: {}", userApiKey);

        // 意图识别
        Map<String, Object> intentMap = routerService.resolveIntent(message, userApiKey);
        Intent intent = (Intent) intentMap.get("intent");
        TargetScope targetScope = (TargetScope) intentMap.get("target_scope");
        String targetName = (String) intentMap.get("target_name");
        if (intent == Intent.UNKNOWN || intent == Intent.NEW_INTENT) {
            log.info("抱歉，目前我无法理解您的问题。？？？？？？？？？？？？您可以尝试换种方式描述您的问题。");
            return Flux.just(messageEvent("抱歉，目前我无法理解您的问题。您可以尝试换种方式描述您的问题。")
                    ,doneEvent());
               }
        List<ToolCallback> toolsToUse = toolService.getToolsForIntent(intent, targetScope);

        return getServerSentEventFluxWithReconfirm(
                userApiKey,
                message,
                conversationId,
                toolsToUse,
                "",
                intent,
                targetScope,
                targetName
        );
    }


    /**
     * 带二次确认检测的流式调用
     */
    private Flux<ServerSentEvent<ChatStreamResponse>> getServerSentEventFluxWithReconfirm(
            String userApiKey,
            String message,
            String conversationId,
            List<ToolCallback> toolsToUse,
            String toolChoice,
            Intent intent,
            TargetScope targetScope,
            String targetName) {

        // 追加时间信息到消息
        String fullMessage = message + " 当前时间：" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Flux<String> llmStream = aiService.chat(userApiKey, fullMessage, conversationId, toolChoice, toolsToUse);

        return Flux.create(sink -> {
                    StringBuilder buffer = new StringBuilder();
                    StringBuilder forwardedContent = new StringBuilder();

                    AtomicBoolean inJsonMode = new AtomicBoolean(false);
                    AtomicInteger braceDepth = new AtomicInteger(0);
                    AtomicBoolean hasSentReconfirm = new AtomicBoolean(false);
                    AtomicBoolean streamCompleted = new AtomicBoolean(false);

                    llmStream.subscribe(
                            chunk -> {
                                if (hasSentReconfirm.get()) {
                                    // 已经发送确认，忽略后续内容
                                    return;
                                }

                                // 检测 JSON 开始（以 { 开头）
                                String trimmed = chunk.trim();
                                if (!inJsonMode.get() && trimmed.startsWith("{")) {
                                    inJsonMode.set(true);
                                    buffer.setLength(0);
                                }

                                if (inJsonMode.get()) {
                                    buffer.append(chunk);
                                    for (char c : chunk.toCharArray()) {
                                        if (c == '{') braceDepth.incrementAndGet();
                                        if (c == '}') braceDepth.decrementAndGet();
                                    }

                                    // 检测到完整的 JSON（braceDepth == 0）
                                    if (braceDepth.get() == 0) {
                                        String jsonStr = buffer.toString().trim();
                                        try {
                                            JsonNode node = objectMapper.readTree(jsonStr);
                                            if ("reconfirm_request".equals(node.get("type").asText())) {
                                                // 解析确认请求
                                                ReconfirmRequest reconfirm = objectMapper.readValue(jsonStr, ReconfirmRequest.class);

                                                // 存入缓存
                                                String newOperationId = UUID.randomUUID().toString();
                                                PendingOperation pending = new PendingOperation();
                                                pending.setOperationId(newOperationId);
                                                pending.setOriginalMessage(message);
                                                pending.setIntent(intent);
                                                pending.setTargetScope(targetScope);
                                                pending.setTargetName(targetName);
                                                pending.setToolName(reconfirm.getToolName());
                                                pending.setToolParams(reconfirm.getToolParams());
                                                pending.setConfirmMessage(reconfirm.getMessage());
                                                pendingCache.save(newOperationId, pending);

                                                // 发送 reconfirm 事件
                                                sink.next(reconfirmEvent(reconfirm.getMessage(), newOperationId));
                                                sink.next(doneEvent());
                                                hasSentReconfirm.set(true);
                                                sink.complete();
                                            }
                                        } catch (Exception e) {
                                            // 不是合法的 JSON 或不是 reconfirm_request
                                            // 把累积的内容作为普通消息发出去
                                            String content = buffer.toString();
                                            if (!content.isEmpty()) {
                                                sink.next(messageEvent(content));
                                                forwardedContent.append(content);
                                            }
                                            buffer.setLength(0);
                                            inJsonMode.set(false);
                                            braceDepth.set(0);
                                        }
                                    }
                                    // JSON 未完整，继续等待
                                } else {
                                    // 普通内容，直接发送
                                    sink.next(messageEvent(chunk));
                                    forwardedContent.append(chunk);
                                }
                            },
                            error -> {
                                if (!hasSentReconfirm.get()) {
                                    sink.next(errorEvent(error.getMessage()));
                                }
                                sink.complete();
                            },
                            () -> {
                                streamCompleted.set(true);
                                if (!hasSentReconfirm.get()) {
                                    // 流正常结束，且没有触发确认
                                    // 如果还有残留内容（可能是未闭合的 JSON 或纯文本）
                                    if (buffer.length() > 0) {
                                        String remaining = buffer.toString().trim();
                                        if (!remaining.isEmpty()) {
                                            sink.next(messageEvent(remaining));
                                        }
                                    }
                                    sink.next(doneEvent());
                                    sink.complete();
                                }
                            }
                    );

                }
        );
    }

    /**
     * 原始流式调用（不带确认检测）
     */
    @NotNull
    private Flux<ServerSentEvent<ChatStreamResponse>> getServerSentEventFlux(
            String userApiKey,
            String message,
            String conversationId,
            List<ToolCallback> toolsToUse,
            String toolChoice) {

        String fullMessage = message + " 当前时间：" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return aiService.chat(userApiKey, fullMessage, conversationId, toolChoice, toolsToUse)
                .map(content -> ServerSentEvent.<ChatStreamResponse>builder()
                        .event("message")
                        .data(ChatStreamResponse.message(content))
                        .build())
                .concatWithValues(ServerSentEvent.<ChatStreamResponse>builder()
                        .event("done")
                        .data(ChatStreamResponse.done())
                        .build())
                .onErrorResume(e -> Flux.just(ServerSentEvent.<ChatStreamResponse>builder()
                        .event("error")
                        .data(ChatStreamResponse.error(e.getMessage()))
                        .build()));
    }


// ========== 事件构造方法 ==========

    private ServerSentEvent<ChatStreamResponse> messageEvent(String content) {
        return ServerSentEvent.<ChatStreamResponse>builder()
                .event("message")
                .data(ChatStreamResponse.message(content))
                .build();
    }

    private ServerSentEvent<ChatStreamResponse> doneEvent() {
        return ServerSentEvent.<ChatStreamResponse>builder()
                .event("done")
                .data(ChatStreamResponse.done())
                .build();
    }

    private ServerSentEvent<ChatStreamResponse> errorEvent(String errorMsg) {
        return ServerSentEvent.<ChatStreamResponse>builder()
                .event("error")
                .data(ChatStreamResponse.error(errorMsg))
                .build();
    }

    private ServerSentEvent<ChatStreamResponse> reconfirmEvent(String content, String operationId) {
        return ServerSentEvent.<ChatStreamResponse>builder()
                .event("reconfirm")
                .data(ChatStreamResponse.reconfirm(content, operationId))
                .build();
    }
}