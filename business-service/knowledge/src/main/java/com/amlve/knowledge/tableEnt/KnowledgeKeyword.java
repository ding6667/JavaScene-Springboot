package com.amlve.knowledge.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 教材关键词表（AI校验用）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_keyword")
public class KnowledgeKeyword extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联章节ID（NULL表示全局关键词）
     */
    @TableField("chapter_id")
    private Long chapterId;

    /**
     * 关联知识点ID（新版表结构新增，可为空）
     */
    @TableField("knowledge_point_id")
    private Long knowledgePointId;

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 关键词释义/说明（新版表结构新增）
     */
    private String description;

    /**
     * 教材页码（新版表结构新增，int 数值序）
     */
    private Integer page;

    /**
     * 同一页内顺序（数字越小越靠前）
     */
    @TableField("page_order")
    private Integer pageOrder;

    /**
     * 同义词（逗号分隔）
     */
    private String synonyms;
}
