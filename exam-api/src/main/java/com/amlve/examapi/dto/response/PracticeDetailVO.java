package com.amlve.examapi.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

/**
 * 刷题详情（不含答案，只返回示例用例）。
 */
@Data
public class PracticeDetailVO {

    private Long id;

    private String content;

    /** 简短题目：一句话概括题意 */
    private String shortTitle;

    /**
     * 前端渲染配置（JSON 对象）：填空 blankCount（要留几个空）、
     * 选择 choiceMode（single=单选，multiple=多选）等，后端已补齐
     */
    private JsonNode renderConfig;

    private Integer type;

    private List<String> options;

    private Long chapterId;

    private Long difficultyConfigId;
    private String difficultyName;

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
