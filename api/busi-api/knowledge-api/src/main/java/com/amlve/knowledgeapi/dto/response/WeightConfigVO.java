package com.amlve.knowledgeapi.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 知识点权重配置 VO（跨模块出参）
 * <p>
 * 由 knowledge 模块的权重配置服务组装并返回，
 * 供出题智能体（agent-question）计算各知识点出题数量，
 * 也用于管理端按章节展示配置列表。
 */
@Data
@Builder
public class WeightConfigVO {

    /**
     * 配置记录ID
     */
    private Long id;

    /**
     * 关联章节ID
     */
    private Long chapterId;

    /**
     * 关联知识点ID
     */
    private Long knowledgePointId;

    /**
     * 知识点名称（关联 knowledge_point 表查出，便于调用方直接展示）
     */
    private String knowledgePointName;

    /**
     * 权重值（1.00-5.00，越大出题占比越高）
     */
    private BigDecimal weight;

    /**
     * 基础出题数量（保证至少出的题数）
     */
    private Integer baseQuestionCount;

    /**
     * 最大出题数量（上限）
     */
    private Integer maxQuestionCount;

    /**
     * 难度分布百分比，格式 "简单,中等,困难"，如 "30,50,20"
     */
    private String difficultyDistribution;

    /**
     * 是否启用（false 的配置出题智能体不采用）
     */
    private Boolean isActive;
}
