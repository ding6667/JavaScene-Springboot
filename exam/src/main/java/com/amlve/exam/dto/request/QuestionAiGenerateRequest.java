package com.amlve.exam.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 出题请求（调用出题智能体）
 */
@Data
public class QuestionAiGenerateRequest {

    /**
     * 章节ID
     */
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    /**
     * 章节名称（可选，用于出题上下文）
     */
    private String chapterName;

    /**
     * 难度配置ID（关联 exam_question_difficulty_config.id）
     */
    @NotNull(message = "难度配置ID不能为空")
    private Long difficultyConfigId;

    /**
     * 出题数量
     */
    @NotNull(message = "出题数量不能为空")
    @Min(value = 1, message = "出题数量至少为1")
    @Max(value = 20, message = "出题数量最多为20")
    private Integer count;

    /**
     * 题型：choice / fill / judge / read / correct / programming / all，默认 all
     */
    private String questionType = "all";

    /**
     * 用户额外指令（可选）
     */
    private String userInstruction;
}
