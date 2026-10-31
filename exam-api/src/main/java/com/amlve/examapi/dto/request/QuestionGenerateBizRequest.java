package com.amlve.examapi.dto.request;

import lombok.Data;

/**
 * AI 出题业务请求（教师/管理员 → exam 模块）。
 */
@Data
public class QuestionGenerateBizRequest {

    /**
     * 章节ID
     */
    private Long chapterId;

    /**
     * 章节名称（可选，前端能带就带；不带时 exam 侧按 chapterId 回查）
     */
    private String chapterName;

    /**
     * 难度配置ID（关联 exam_question_difficulty_config.id）
     */
    private Long difficultyConfigId;

    /**
     * 题型：1~7，all 表示全部
     */
    private String questionType = "all";

    /**
     * 出题数量
     */
    private Integer count;

    /**
     * 用户额外指令（可选）
     */
    private String userInstruction;
}
