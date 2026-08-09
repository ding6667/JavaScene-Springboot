package com.amlve.ai.controller;

import com.amlve.ai.enumeration.Intent;
import com.amlve.ai.service.RouterLlmService;
import com.amlve.ai.vo.ChatBlockResponse;
import com.amlve.ai.vo.ChatStreamResponse;
import com.amlve.ai.service.RagService;
import com.amlve.ai.service.tools.IndividualSearchTools;
import com.amlve.common.request.Result;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;


@RequestMapping("/api/chat")
@RestController
@RequiredArgsConstructor
public class ChatController {

    @Qualifier("specialChatClient")
    private final ChatClient specialModel;
    @Qualifier("commonChatClient")
    private final ChatClient commonModel;
    private final IndividualSearchTools ragTools;
    private final ChatMemory chatMemory;
    private final RagService ragService;
    private final RouterLlmService routerService;



    @GetMapping(value = "/blockChat", produces = "application/json;charset=UTF-8")
    public Result<ChatBlockResponse> blockChat(@RequestParam("message") String message, @RequestParam("conversationId") String conversationId) {
        long start = System.currentTimeMillis();
        ChatResponse response = commonModel.prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(ragTools)
                .call()
                .chatResponse();

        String content = response.getResult().getOutput().getText();
        // 从 ChatResponse 中提取 token 等元数据
        Usage usage = response.getMetadata().getUsage();

        return Result.success(ChatBlockResponse.builder()
                .content(content)
                .model("deepseek-chat")
                .inputTokens(usage.getPromptTokens())
                .outputTokens(usage.getCompletionTokens())
                .costMs(System.currentTimeMillis() - start)
                .conversationId(conversationId)
                .build());
    }

    @GetMapping(value = "/streamChat",produces = "text/html;charset=UTF-8")
    public Flux<ServerSentEvent<ChatStreamResponse>> streamChat(@RequestParam("message") String message, String conversationId){
        Intent intent = routerService.resolveIntent(message);
        List<ToolCallback> toolsToUse = routerService.getToolsForIntent(intent);

        if(toolsToUse.isEmpty()){
            return getServerSentEventFlux(message, conversationId, toolsToUse, commonModel, "");
        } else if (intent == Intent.UNKNOWN) {
            return getServerSentEventFlux(message, conversationId, toolsToUse, commonModel, "\n本次对话必须使用answer工具的结果作为完整的回答");
        }
        return getServerSentEventFlux(message, conversationId, toolsToUse, specialModel, "");
    }

    @NotNull
    private Flux<ServerSentEvent<ChatStreamResponse>> getServerSentEventFlux(
            @RequestParam("message") String message
            , String conversationId
            , List<ToolCallback> toolsToUse
            , ChatClient model
            , String toolChoice) {
        return model.prompt()
                .user(message + toolChoice)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(toolsToUse)
                .stream()
                .content()
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


}
