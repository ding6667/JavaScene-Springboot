package com.amlve.examapi.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 题目完整详情（内部模块用，含答案）
 */
@Data
public class QuestionDetailDTO {

    private Long id;
    private String content;
    private Integer type;
    private List<String> options;
    private String answer;
    private String analysis;
    private Long chapterId;
    private Integer difficulty;
    private Integer score;
    private String codeFramework;
    private List<String> requirements;
    private List<TestCaseDTO> testCases;
    private Integer judgeMode;

    @Data
    public static class TestCaseDTO {
        private Integer caseIndex;
        private String input;
        private String expectedOutput;
        private Boolean isSample;
        private String caseType;
        private Integer timeLimit;
        private Integer memoryLimit;
    }
}
