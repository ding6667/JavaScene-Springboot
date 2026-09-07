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

    @TableId(type = IdType.AUTO)
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
    @TableField("page_ref")
    private String pageRef;
}
