package com.amlve.examapi.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

/**
 * 刷题列表项（不含答案）。
 */
@Data
public class PracticeItemVO {

    private Long id;

    private String content;

    /** 简短题目：一句话概括题意，列表卡片直接用 */
    private String shortTitle;

    /**
     * 前端渲染配置（JSON 对象）：填空 blankCount、选择 choiceMode 等；
     * 后端已按题型补齐，前端按此渲染即可
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
     * 通过率（%）
     */
    private Double acceptanceRate;

    /**
     * 总提交次数
     */
    private Integer submitCount;

    /**
     * 学生是否已做
     */
    private Boolean done;

    /**
     * 做对/做错
     */
    private Boolean correct;

    /**
     * 学生尝试次数
     */
    private Integer attemptCount;

    /**
     * 学生是否收藏
     */
    private Boolean favorited;
}
