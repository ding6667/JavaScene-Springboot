package com.amlve.examapi.dto.request;

import lombok.Data;

/**
 * 刷题列表查询请求。
 */
@Data
public class PracticeQueryRequest {

    /**
     * 章节ID（可选）
     */
    private Long chapterId;

    /**
     * 题型（可选）
     */
    private Integer type;

    /**
     * 难度（可选）
     */
    private Long difficultyConfigId;

    /**
     * 是否只查未做过的题
     */
    private Boolean onlyUndone = false;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
