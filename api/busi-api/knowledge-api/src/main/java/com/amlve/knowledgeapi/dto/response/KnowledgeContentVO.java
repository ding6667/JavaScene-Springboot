package com.amlve.knowledgeapi.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教材原文列表项
 * <p>
 * 章节名与知识点名为关联查询所得的冗余字段，避免调用方为每条素材再查一次名称。
 */
@Data
public class KnowledgeContentVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 所属章节ID
     */
    private Long chapterId;

    /**
     * 章节名称（冗余返回）
     */
    private String chapterName;

    /**
     * 关联知识点ID
     */
    private Long knowledgePointId;

    /**
     * 知识点名称（关联查询所得）
     */
    private String knowledgePointName;

    /**
     * 内容类型码值：1=概念定义，2=例题，3=代码案例，4=习题
     */
    private Integer contentType;

    /**
     * 内容类型中文描述
     */
    private String contentTypeName;

    /**
     * 教材原文
     */
    private String content;

    /**
     * 教材页码
     */
    private Integer page;

    /**
     * 同一页内顺序
     */
    private Integer pageOrder;

    /**
     * Dify 文档ID（未同步时为空）
     */
    private String difyDocumentId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
