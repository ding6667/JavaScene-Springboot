package com.amlve.examapi.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 刷题列表项（不含答案）。
 */
@Data
public class PracticeItemVO {

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
