package com.amlve.agentquestionapi.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class QuestionGenerateResponse {

    private List<QuestionItem> questions;
    private Integer total;

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
    }
}