package com.amlve.dialogue.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatStreamResponse {

    /**
     * 事件类型：message-消息内容 | done-完成 | error-错误
     */
    private String event;

    /**
     * 事件内容
     */
    private String content;

    public static ChatStreamResponse message(String content) {
        return ChatStreamResponse.builder()
                .event("message")
                .content(content)
                .build();
    }

    public static ChatStreamResponse done() {
        return ChatStreamResponse.builder()
                .event("done")
                .content("")
                .build();
    }

    public static ChatStreamResponse error(String errorMsg) {
        return ChatStreamResponse.builder()
                .event("error")
                .content(errorMsg)
                .build();
    }
}