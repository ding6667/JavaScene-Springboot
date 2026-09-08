package com.amlve.knowledge.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 教材原文内容表（AI检索用）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_content")
public class KnowledgeContent extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联章节ID
     */
    @TableField("chapter_id")
    private Long chapterId;

    /**
     * 关联知识点ID（可为空）
     */
    @TableField("knowledge_point_id")
    private Long knowledgePointId;

    /**
     * 教材原文摘录
     */
    private String content;

    /**
     * 类型（1=概念定义，2=例题，3=代码案例，4=习题）
     */
    @TableField("content_type")
    private Integer contentType;

    /**
     * 页码
     */
    @TableField("page_ref")
    private String pageRef;

    /**
     * Dify知识库文档ID
     */
    @TableField("dify_document_id")
    private String difyDocumentId;
}
