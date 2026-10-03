package com.amlve.examapi.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 题目基本信息（内部模块用，不含答案）
 */
@Data
public class QuestionBaseResponse {

    private Long id;
    private String content;
    private Integer type;
    private List<String> options;
    private List<String> requirements;
    private String codeFramework;
    private Long chapterId;
    private Integer difficulty;
    private Integer score;
    private Integer judgeMode;
}