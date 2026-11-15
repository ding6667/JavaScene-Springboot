package com.amlve.knowledgeapi.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识点 DTO（跨模块传输）
 */
@Data
public class KnowledgePointDTO {

    private Long id;

    /**
     * 关联章节ID
     */
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
    private String pageRef;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
