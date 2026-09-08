package com.amlve.knowledge.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 章节信息响应
 */
@Data
public class ChapterResponse {

    private Long id;

    /**
     * 所属课程ID
     */
    private Long courseId;

    /**
     * 父章节ID（0表示顶级章）
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
     * 层级（1=章，2=节，3=小节）
     */
    private Integer level;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 章节简介
     */
    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
