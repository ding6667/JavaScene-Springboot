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

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联章节ID（NULL表示全局关键词）
     */
    @TableField("chapter_id")
    private Long chapterId;

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 同义词（逗号分隔）
     */
    private String synonyms;
}
