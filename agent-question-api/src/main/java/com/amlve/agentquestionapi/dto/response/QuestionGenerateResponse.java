package com.amlve.agentquestionapi.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class QuestionGenerateResponse {

    private List<QuestionItem> questions;
    private Integer total;
    private String status;
    private String errorMsg;

    @Data
    @Builder
    public static class QuestionItem {
        private String id;
        private String type;          // choice / fill / judge / read / correct / programming
        private String difficulty;    // basic / intermediate / advanced
        private String knowledgePoint;
        private List<String> knowledgePoints;
        private String content;
        /** 简短题目：一句话概括题意（如「最长k个相等字符串」），列表/卡片展示用 */
        private String shortTitle;
        /**
         * 前端渲染配置（JSON 对象）：填空 {@code {"blankCount":3}}、
         * 选择 {@code {"choiceMode":"single"|"multiple"}}、编程 {@code {"language":"java"}}
         */
        private JsonNode renderConfig;
        private List<String> options; // 仅选择题需要
        private String answer;
        private String analysis;      // 含页码引用
        private BigDecimal difficultyScore;   // Dify 难度分
        private String cognitiveLevel;    // 认知层次
        private List<String> requirements; // 主观题要求
        private String codeFramework;      // 编程题代码骨架
        private List<String> hints;        // 阶梯提示
        private List<TestCase> testCases;  // 测试用例
        private List<String> relatedQuestions; // 相似题推荐
        private List<String> abilityDimensions; // 能力维度
        private List<String> commonMistakes;    // 常见错误
        private String solutionOutline;    // 解题思路概述
        private BigDecimal weight;         // 组卷权重
    }

    @Data
    @Builder
    public static class TestCase {
        private Integer caseIndex;
        private String input;
        private String expectedOutput;
        private Boolean isSample;
        private String caseType;
        private Integer weight;
        private Integer timeLimit;
        private Integer memoryLimit;
        private String description;
    }
}
