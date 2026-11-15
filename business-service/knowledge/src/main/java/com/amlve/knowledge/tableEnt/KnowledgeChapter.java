package com.amlve.knowledge.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 教材章节表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_chapter")
public class KnowledgeChapter extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属课程ID
     */
    @TableField("course_id")
    private Long courseId;

    /**
     * 父章节ID（0表示顶级章）
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 章节编号（如 3.2）
     */
    @TableField("chapter_code")
    private String chapterCode;

    /**
     * 所属大章（新版表结构新增；列名为 code 但类型是 bigint，以表结构为准）
     */
    @TableField("top_chapter_code")
    private Long topChapterCode;

    /**
     * 章节标题
     */
    private String title;

    /**
     * 层级（1=章，2=节，3=小节）
     */
    private Integer level;

    /**
     * 排序序号
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 章节简介
     */
    private String description;

    /**
     * 章节摘要（text 列，与 description 区分：前者为简介，后者为较长摘要）
     */
    private String summary;
}
