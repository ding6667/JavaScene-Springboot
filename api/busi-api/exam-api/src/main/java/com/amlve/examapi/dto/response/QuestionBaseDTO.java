package com.amlve.examapi.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import java.util.List;

/**
 * 题目基本信息（内部模块用，不含答案）
 */
@Data
public class QuestionBaseDTO {

    private Long id;
    private String content;
    /** 简短题目（一句话概括，列表展示用） */
    private String shortTitle;
    /** 前端渲染配置（JSON 对象）：填空 blankCount、选择 choiceMode 等 */
    private JsonNode renderConfig;
    private Integer type;
    private List<String> options;
    private List<String> requirements;
    private String codeFramework;
    private Long chapterId;
    private Long difficultyConfigId;
    private String difficultyName;
    private Integer score;
    private Integer judgeMode;
}
