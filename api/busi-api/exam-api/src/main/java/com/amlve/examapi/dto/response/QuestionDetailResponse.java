package com.amlve.examapi.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import java.util.List;

/**
 * 题目完整详情（内部模块用，含答案）
 */
@Data
public class QuestionDetailResponse {

    private Long id;
    private String content;
    /** 简短题目（一句话概括，作业快照/卡片展示用） */
    private String shortTitle;
    /**
     * 前端渲染配置（JSON 对象）：填空 blankCount、选择 choiceMode 等；
     * 发布作业时随题目一起固化到快照表
     */
    private JsonNode renderConfig;
    private Integer type;
    private List<String> options;
    private String answer;
    private String analysis;
    private Long chapterId;
    private Long difficultyConfigId;
    private String difficultyName;
    private Integer score;
    private String codeFramework;
    /** 知识点ID列表（发布作业时随题目快照一起固化） */
    private List<Long> knowledgePointIds;
    private List<String> requirements;
    /** 阶梯提示（发布作业时固化，学生按需展开；不直接暴露给学生端列表） */
    private List<String> hints;
    /** 解题思路概述（与 analysis 分开控制，做错后才展示） */
    private String solutionOutline;
    private List<TestCaseItem> testCases;
    private Integer judgeMode;

    @Data
    public static class TestCaseItem {
        private Integer caseIndex;
        private String input;
        private String expectedOutput;
        private Boolean isSample;
        private String caseType;
        private Integer timeLimit;
        private Integer memoryLimit;
    }
}