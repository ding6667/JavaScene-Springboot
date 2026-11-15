package com.amlve.knowledge.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 教材知识点表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_point")
public class KnowledgePoint extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联章节ID
     */
    @TableField("chapter_id")
    private Long chapterId;

    /**
     * 知识点名称
     */
    private String name;

    /**
     * 知识点描述/定义
     */
    private String description;

    /**
     * 难度（1=了解，2=理解，3=掌握，4=重难点）
     */
    private Integer difficulty;

    /**
     * 关键词（逗号分隔）
     */
    private String keyword;

    /**
     * 教材页码（如 P.123-125）
     */
    /**
     * 教材页码（如 P.123-125）。知识点表保留 varchar 口径，
     * 与 knowledge_content 的 int page 不同，以新表结构为准
     */
    @TableField("page_ref")
    private String pageRef;

    /**
     * 同一页内顺序（数字越小越靠前，新版表结构新增）
     */
    @TableField("page_order")
    private Integer pageOrder;
}
