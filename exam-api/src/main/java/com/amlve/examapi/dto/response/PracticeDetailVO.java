package com.amlve.examapi.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 刷题详情（不含答案，只返回示例用例）。
 */
@Data
public class PracticeDetailVO {

    private Long id;

    private String content;

    private Integer type;

    private List<String> options;

    private Long chapterId;

    private Integer difficulty;

    private Integer score;

    private String codeFramework;

    private List<String> requirements;

    /**
     * 示例测试用例（仅 isSample=true）
     */
    private List<TestCaseVO> sampleTestCases;

    /**
     * 通过率
     */
    private Double acceptanceRate;

    /**
     * 学生是否已做
     */
    private Boolean done;

    /**
     * 学生上次提交的答案
     */
    private String lastAnswer;

    @Data
    public static class TestCaseVO {
        private String input;
        private String expectedOutput;
        private String description;
    }
}
