package com.amlve.agentqaapi.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答历史列表项
 */
@Data
public class QaHistoryVO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 章节ID（自由提问为 null）
     */
    private Long chapterId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 轮次序号（同一会话内按它还原完整对话）
     */
    private Integer roundNo;

    /**
     * 学生问题
     */
    private String question;

    /**
     * AI 回答
     */
    private String answer;

    /**
     * 关联知识点，逗号分隔
     */
    private String knowledgePoints;

    /**
     * 教材原文引用
     */
    private String referenceText;

    /**
     * 是否贴合教材：1=是，0=超纲，null=未判定（该章未配关键词或教材原文）
     */
    private Integer isTextbookMatch;

    /**
     * 本轮消耗 token 数
     */
    private Integer tokensUsed;

    /**
     * 端到端响应耗时（毫秒）
     */
    private Integer responseTime;

    /**
     * 提问时间
     */
    private LocalDateTime createTime;
}
