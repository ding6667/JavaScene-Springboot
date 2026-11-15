package com.amlve.agentquestionapi.dto.request;

import com.amlve.common.constant.ErrorMessageConstant;
import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
public class QuestionGenerateRequest {

    @NotNull(message = ErrorMessageConstant.CHAPTER_NOT_SELECTED)
    private Long chapterId;

    /**
     * 章节名称（传给 Dify 用于上下文）
     */
    private String chapterName;

    /**
     * 难度配置（完整传入 Dify，用于约束出题标准）
     */
    private DifficultyConfig difficulty;

    /**
     * 题型：choice / fill / judge / read / correct / programming / all
     */
    private String questionType = "all";

    @NotNull(message = ErrorMessageConstant.QUESTION_COUNT_INVALID)
    @Min(value = 1, message = ErrorMessageConstant.QUESTION_COUNT_INVALID)
    @Max(value = 20, message = ErrorMessageConstant.QUESTION_COUNT_INVALID)
    private Integer count;

    /**
     * 用户额外指令（可选）
     */
    private String userInstruction;

    /**
     * 知识点分配（由 QuestionAgentImpl 计算后传入），结构 [{pointId, name, count}]
     */
    private List<AllocationItem> allocation;


    /**
     * 难度配置静态内部类（直接传给 Dify）
     */
    @Data
    public static class DifficultyConfig {

        /**
         * 难度编码，如 basic / medium / hard
         */
        private String code;

        /**
         * 难度名称，如 简单、中等、困难
         */
        private String name;

        /**
         * 难度说明
         */
        private String description;

        /**
         * 题目示例
         */
        private String example;

        /**
         * 认知层次，如 ["记忆","理解"]
         */
        private List<String> cognitiveLevels;


    }

    /**
     * 知识点分配项
     */
    @Data
    public static class AllocationItem {
        private String pointId;
        private String name;
        private String count;
    }
}