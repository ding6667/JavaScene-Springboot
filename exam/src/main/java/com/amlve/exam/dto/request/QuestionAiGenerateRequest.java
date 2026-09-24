package com.amlve.exam.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * AI 出题请求（调用出题智能体）
 */
@Data
public class QuestionAiGenerateRequest {

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    /**
     * 章节名称（可选，用于出题上下文）
     */
    private String chapterName;

    @NotNull(message = "出题数量不能为空")
    @Min(value = 1, message = "出题数量至少为1")
    @Max(value = 20, message = "出题数量最多为20")
    private Integer count;

    /**
     * 题型：choice / fill / judge / read / correct / programming / all
     */
    private String questionType = "all";

    /**
     * 难度配置（可选）
     */
    private DifficultyConfig difficulty;

    /**
     * 用户额外指令（可选）
     */
    private String userInstruction;

    @Data
    public static class DifficultyConfig {
        private String code;
        private String name;
        private String description;
        private String example;
        private List<String> cognitiveLevels;
    }
}