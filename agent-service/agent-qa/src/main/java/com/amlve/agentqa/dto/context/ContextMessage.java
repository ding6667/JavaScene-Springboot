package com.amlve.agentqa.dto.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话上下文消息（Redis 存储单元，一条消息一个 JSON 元素）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContextMessage {

    /**
     * 角色：user / assistant
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 写入时间戳（秒），仅用于排查"上下文里这条是哪一问"
     */
    private Long time;
}
