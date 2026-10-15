package com.amlve.examapi.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题本条目。
 */
@Data
public class WrongQuestionVO {

    private Long id;

    /** 题目ID */
    private Long questionId;

    /** 题干 */
    private String content;

    /** 题型 */
    private Integer type;

    /** 难度 */
    private Integer difficulty;

    /** 累计错误次数 */
    private Integer wrongCount;

    /** 最近一次错误答案 */
    private String lastAnswer;

    /** 是否已掌握：0=未掌握，1=已掌握 */
    private Integer mastered;

    /** 最近答错时间 */
    private LocalDateTime lastWrongTime;
}
