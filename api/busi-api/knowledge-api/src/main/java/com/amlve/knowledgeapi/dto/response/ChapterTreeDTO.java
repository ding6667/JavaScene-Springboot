package com.amlve.knowledgeapi.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 章节树节点（按 parentId 组装，含子节点）
 * <p>
 * 字段名对齐 knowledge_chapter 表：章节标题列名是 title（不是文档里的 chapterName）。
 */
@Data
public class ChapterTreeDTO {

    /**
     * 章节ID
     */
    private Long id;

    /**
     * 所属课程ID
     */
    private Long courseId;

    /**
     * 父章节ID（顶级为 0）
     */
    private Long parentId;

    /**
     * 章节编号（如 3.2）
     */
    private String chapterCode;

    /**
     * 章节标题
     */
    private String title;

    /**
     * 层级：1=章，2=节，3=小节
     */
    private Integer level;

    /**
     * 同级排序序号
     */
    private Integer sortOrder;

    /**
     * 子节点列表（无子节点时为空列表，便于前端直接遍历）
     */
    private List<ChapterTreeDTO> children;
}
