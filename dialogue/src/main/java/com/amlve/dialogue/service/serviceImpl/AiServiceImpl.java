package com.amlve.dialogue.service.serviceImpl;

import com.amlve.dialogue.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    @Value("${spring.ai.openai.chat.options.temperature}")
    private Double temperature;
    @Value("${spring.ai.openai.chat.options.frequency-penalty}")
    private Double frequencyPenalty;
    @Value("${spring.ai.openai.chat.options.presence-penalty}")
    private Double presencePenalty;
    @Value("${spring.ai.openai.chat.options.max-tokens}")
    private Integer maxTokens;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;
    @Value("${spring.ai.openai.base-url}")
    private String apiBase;

    public ChatClient createChatClient(String model,String apiBase,String apiKey) {
        return ChatClient.builder(OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(apiBase)
                        .apiKey(apiKey)
                        .build()
                ).defaultOptions(OpenAiChatOptions.builder()
                                .model(model)
                                .temperature(temperature)
                                .frequencyPenalty(frequencyPenalty)
                                .presencePenalty(presencePenalty)
                                .maxTokens(maxTokens)
                                .build())
                .build())
                .build();
    }

    public ChatClient createChatClient(String apiKey) {
        return createChatClient(model,apiBase,apiKey);
    }


    public Flux<String> chat(String userApiKey, String userPrompt , String conversationId, String toolChoice , List<ToolCallback> toolsToUse) {
        var spec = createChatClient(userApiKey)
                .prompt()
                .user(userPrompt + toolChoice)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId));
        if (toolsToUse != null && !toolsToUse.isEmpty()) {
            spec = spec.toolCallbacks(toolsToUse);
        }
        return spec.stream().content();
    }

}
