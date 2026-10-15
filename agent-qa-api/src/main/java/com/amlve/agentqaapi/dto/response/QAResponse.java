package com.amlve.agentqaapi.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 答疑结果
 * <p>
 * 除答案本体外，还回传会话标识与本轮元数据：sessionId / roundNo 供前端续接同一会话，
 * references 与 isTextbookMatch 是"教材原文溯源"和超纲统计的数据来源。
 */
@Data
public class QAResponse {

    /**
     * AI 回答（已剥除模型思考链）
     */
    private String answer;

    /**
     * 教材原文引用（命中关键词重合度最高的片段，最多 500 字）。
     * <p>
     * 可能为空：本章节未配置关键词/教材原文，或回答未命中任何关键词时为空，
     * 此时宁缺不假——编一段"出处"正是本任务要防的幻觉
     */
    private List<String> references;

    /**
     * 是否贴合教材：null 表示无法判定（该章未配关键词或教材原文），true/false 由命中率阈值给出
     */
    private Boolean isTextbookMatch;

    /**
     * 关键词命中率，保留两位小数（与 isTextbookMatch 同源）
     */
    private Double hitRate;

    /**
     * 命中的本章知识点名称，逗号分隔（供学情分析按知识点统计提问量）
     */
    private String knowledgePoints;

    /**
     * 会话ID（首轮由后端生成）
     */
    private String sessionId;

    /**
     * 本轮在同一会话内的序号，从 1 递增
     */
    private Integer roundNo;
}
