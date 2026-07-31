package com.amlve.ai.controller;

import com.amlve.ai.configuration.DeepSeekModelConfig;
import com.amlve.ai.service.RagService;
import com.amlve.ai.util.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@RequestMapping("/api/chat")
@RestController
public class ChatController {
    private final ChatClient deepSeekModel;

    private final RagService ragService;

    @RequestMapping(value = "/chat",produces = "text/html;charset=UTF-8")
    public Flux<String> message(@RequestParam("message") String message, String conversationId){
        return deepSeekModel.prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(new Tool())
                .stream()
                .content();
    }

    @RequestMapping(value = "/chat1",produces = "text/html;charset=UTF-8")
    public void message(){
         ragService.find();
    }
}
