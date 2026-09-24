package com.amlve.agentquestionapi.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class QuestionGenerateResponse {

    private List<QuestionItem> questions;
    private Integer total;
    private String status;

    @Data
    @Builder
    public static class QuestionItem {
        private String id;
        private String type;          // choice / fill / judge / read / correct / programming
        private String difficulty;    // basic / intermediate / advanced
        private String knowledgePoint;
        private String content;
        private List<String> options; // 仅选择题需要
        private String answer;
        private String analysis;      // 含页码引用
        private Double difficultyScore;   // Dify 难度分
        private String cognitiveLevel;    // 认知层次
        private List<String> requirements; // 编程题要求
        private String codeFramework;      // 编程题代码骨架
    }
}