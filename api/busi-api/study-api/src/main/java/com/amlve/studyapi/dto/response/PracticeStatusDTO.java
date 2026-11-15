package com.amlve.studyapi.dto.response;

import lombok.Data;

/**
 * 学生题目状态（供 exam 拼装刷题列表 / 详情页）
 *
 * <p>口径（任务书 5.4 定稿）：done / correct 只从 study_question_status 表取，
 * 避免一题多轮运行导致列表抖动；lastAnswer 取 study_practice_record 最新答案。
 */
@Data
public class PracticeStatusDTO {

    private Long questionId;

    /**
     * 是否做过（状态表 status >= 1）
     */
    private Boolean done;

    /**
     * 是否做对（状态表 status == 2 已通过）
     */
    private Boolean correct;

    /**
     * 尝试次数
     */
    private Integer attemptCount;

    /**
     * 上次答案
     */
    private String lastAnswer;

    /**
     * 是否收藏（收藏功能暂未开放，固定 false）
     */
    private Boolean favorited;
}
