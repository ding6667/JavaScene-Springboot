package com.amlve.knowledge.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 知识点权重配置表实体类
 * <p>
 * 存储每个知识点在自动出题时的权重与出题策略，
 * 由管理员维护，出题智能体通过 knowledge-api 的
 * {@link com.amlve.knowledgeapi.service.KnowledgeService#getWeightConfigsByChapter(Long)} 读取。
 * <p>
 * 同一章节 + 知识点组合唯一（数据库唯一键 uk_chapter_point）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_weight_config")
public class KnowledgeWeightConfig extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联章节ID（knowledge_chapter.id）
     */
    @TableField("chapter_id")
    private Long chapterId;

    /**
     * 关联知识点ID（knowledge_point.id）
     */
    @TableField("knowledge_point_id")
    private Long knowledgePointId;

    /**
     * 权重值（1.00-5.00，越大出题占比越高）
     */
    private BigDecimal weight;

    /**
     * 基础出题数量（保证至少出的题数）
     */
    @TableField("base_question_count")
    private Integer baseQuestionCount;

    /**
     * 最大出题数量（上限，防止某个知识点刷屏）
     */
    @TableField("max_question_count")
    private Integer maxQuestionCount;

    /**
     * 难度分布百分比，格式如 "30,50,20"（简单,中等,困难）
     */
    @TableField("difficulty_distribution")
    private String difficultyDistribution;

    /**
     * 是否启用（1=启用，0=禁用；禁用的配置出题智能体不采用）
     */
    @TableField("is_active")
    private Integer isActive;
}
