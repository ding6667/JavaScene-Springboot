package com.amlve.knowledgeapi.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 教材原文分页查询条件
 * <p>
 * 分页字段沿用 common 模块的口径（pageNum 从 1 开始 / pageSize 每页条数）。
 */
@Data
public class KnowledgeContentQueryRequest {

    /**
     * 所属章节ID
     */
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    /**
     * 内容类型：1=概念定义，2=例题，3=代码案例，4=习题；为空查全部
     */
    private Integer contentType;

    /**
     * 正文关键词（可选，模糊匹配）
     */
    private String keyword;

    /**
     * 页码，从 1 开始
     */
    @Min(value = 1, message = "页码最小为 1")
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    @Min(value = 1, message = "每页条数最小为 1")
    private Integer pageSize = 10;
}
