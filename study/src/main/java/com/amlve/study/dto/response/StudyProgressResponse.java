package com.amlve.study.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习进度响应
 */
@Data
public class StudyProgressResponse {

    private Long id;

    /**
     * 章节ID
     */
    private Long chapterId;

    /**
     * 章节标题（来自 knowledge-api，非直查表）
     */
    private String chapterName;

    /**
     * 本章节学习进度(0-100)
     */
    private Integer progressRate;

    /**
     * 累计学习时长
     */
    private Integer studyDuration;

    /**
     * 是否完成(0->未完成,1->已完成)
     */
    private Integer isCompleted;

    /**
     * 最近学习时间
     */
    private LocalDateTime lastStudyTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
