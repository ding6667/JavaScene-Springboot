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

    @TableId(type = IdType.ASSIGN_ID)
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
     * 教材页码（新版表结构由 page_ref varchar 改为 page int，排序按数值序而非字典序）
     */
    private Integer page;

    /**
     * 同一页内顺序（数字越小越靠前），与 page 联合决定取素材顺序
     */
    @TableField("page_order")
    private Integer pageOrder;

    /**
     * Dify知识库文档ID（先入库后同步 Dify，故允许为空）
     */
    @TableField("dify_document_id")
    private String difyDocumentId;
}
