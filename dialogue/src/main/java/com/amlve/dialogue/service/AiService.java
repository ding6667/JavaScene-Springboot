package com.amlve.dialogue.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AiService {
    ChatClient createChatClient(String model,String apiBase,String apiKey);
    Flux<String> chat(String userApiKey, String userPrompt , String conversationId, String toolChoice , List<ToolCallback> toolsToUse);
    ChatClient createChatClient(String apiKey);
}
