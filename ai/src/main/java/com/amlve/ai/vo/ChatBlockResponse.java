package com.amlve.ai.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.AllArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBlockResponse {
    /** 回复内容 */
    private String content;
    /** 使用的模型 */
    private String model;
    /** 本次消耗的输入 token */
    private Integer inputTokens;
    /** 本次消耗的输出 token */
    private Integer outputTokens;
    /** 总耗时（毫秒） */
    private Long costMs;
    /** 会话 ID */
    private String conversationId;
}