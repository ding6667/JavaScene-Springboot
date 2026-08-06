package com.amlve.ai.controller;

import com.amlve.ai.service.RagService;
import com.amlve.ai.util.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@RequestMapping("/api/chat")
@RestController
public class ChatController {

    @Autowired(required = false)
    private ChatClient deepSeekModel;

    @Autowired(required = false)
    private RagService ragService;

    @Autowired(required = false)
    private RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    @RequestMapping(value = "/chat",produces = "text/html;charset=UTF-8")
    public Flux<String> message(@RequestParam("message") String message, String conversationId){
        return deepSeekModel.prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(new Tool())
                .stream()
                .content();
    }

    @RequestMapping(value = "/answer",produces = "text/html;charset=UTF-8")
    public Flux<String> answer(@RequestParam("message") String message){
        return deepSeekModel.prompt()
                .user(message)
                .advisors(retrievalAugmentationAdvisor)
                .stream()
                .content();
    }

}
