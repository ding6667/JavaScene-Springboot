package com.amlve.knowledgeapi.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 章节新增 / 更新请求（id 为空则新增）
 * <p>
 * 字段名以 knowledge_chapter 表为准：章节标题列名是 title，
 * 任务文档里的 chapterName 在库中并不存在。
 */
@Data
public class KnowledgeChapterSaveRequest {

    /**
     * 章节ID，为空则新增
     */
    private Long id;

    /**
     * 所属课程ID
     */
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    /**
     * 父章节ID，顶级章节传 0 或不传
     */
    private Long parentId;

    /**
     * 章节编号（如 3.2）
     */
    @NotBlank(message = "章节编号不能为空")
    @Size(max = 20, message = "章节编号长度不能超过 20")
    private String chapterCode;

    /**
     * 章节标题，如 "第 2 章 Java 编程基础"
     */
    @NotBlank(message = "章节标题不能为空")
    @Size(max = 100, message = "章节标题长度不能超过 100")
    private String title;

    /**
     * 层级：1=章，2=节，3=小节；不传则按父章节自动推导
     */
    @Min(value = 1, message = "层级最小为 1")
    private Integer level;

    /**
     * 同级排序序号
     */
    @Min(value = 0, message = "排序序号不能为负数")
    private Integer sortOrder;

    /**
     * 章节简介
     */
    private String description;

    /**
     * 章节摘要
     */
    private String summary;
}
