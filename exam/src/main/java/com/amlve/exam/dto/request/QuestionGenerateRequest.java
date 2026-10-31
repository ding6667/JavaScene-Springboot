package com.amlve.exam.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 出题请求（按章节随机抽取）
 */
@Data
public class QuestionGenerateRequest {

    /** 章节 ID */
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;


    /** 题型（多选，1=选择 2=填空 3=判断 4=程序阅读 5=编程） */
    @NotNull(message = "题型不能为空")
    private List<TypeAllocation> typeAllocations;

    /**
     * 题型分配：一个题型一组题量。
     * <p>
     * 可在题型内再按难度档分配（difficultyAllocations），例如"选择题 3 道 = 简单 1 道 + 拔高 2 道"；
     * 传了难度配比时以各档数量为准，顶层 count 忽略。
     */
    @Data
    public static class TypeAllocation {
        private Integer type;
        /** 该题型总题数（未配难度档时使用；配了难度档则忽略） */
        private Integer count;
        /** 难度配比，可空 */
        private List<DifficultyAllocation> difficultyAllocations;
    }

    /** 单档难度分配 */
    @Data
    public static class DifficultyAllocation {
        /** 难度配置ID（exam_question_difficulty_config.id），取自 /api/exam/difficulty-config/list */
        private Long difficultyConfigId;
        /** 该难度取几道；为空表示该档全取 */
        private Integer count;
    }
}