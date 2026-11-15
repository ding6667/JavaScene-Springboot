package com.amlve.examapi.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题本条目。
 * <p>
 * 题面一律取自「错题记录指向的题目快照」，不实时读题库：
 * 学生在作业里做错的那一版题，过多久回看都还是那一版。
 */
@Data
public class WrongQuestionVO {

    /** 错题记录ID（标记掌握、重做时用它，不要用 questionId） */
    private Long id;

    /** 题目快照ID（exam_question_snapshot.id） */
    private Long snapshotId;

    /** 快照来源：1=题库版本，2=作业发布快照 */
    private Integer snapshotType;

    /** 题库题目ID（exam_question.id），用于跳转题库详情 */
    private Long questionId;

    /** 题干（快照） */
    private String content;

    /** 简短题目：一句话概括题意，错题列表直接用 */
    private String shortTitle;

    /** 题型（快照） */
    private Integer type;

    /** 难度（快照） */
    private Long difficultyConfigId;
    private String difficultyName;

    /** 来源：1=作业，2=刷题 */
    private Integer source;

    /** 累计错误次数 */
    private Integer wrongCount;

    /** 最近一次错误答案 */
    private String lastAnswer;

    /** 是否已掌握：0=未掌握，1=已掌握 */
    private Integer mastered;

    /**
     * 题面是否已过期：题库来源的错题，如果题库把题改了就会置 true。
     * 作业来源恒为 false——作业本来就该按发布那一版看。
     */
    private Boolean outdated;

    /** 最近答错时间 */
    private LocalDateTime lastWrongTime;
}
