package com.amlve.examapi.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生「我的作业」列表项。
 * <p>
 * 作业基础信息来自 teach 模块的作业快照元数据（不含答案），
 * 提交进度与批改结果来自 exam 侧的作答记录与批改任务表。
 */
@Data
public class StudentHomeworkItemVO {

    private Long homeworkId;

    private String title;

    /** 作业要求/说明 */
    private String content;

    private Long chapterId;

    /** 章节名（依赖知识模块，暂为空） */
    private String chapterName;

    /** 题目数量 */
    private Integer questionCount;

    /** 作业总分 */
    private Integer totalScore;

    private LocalDateTime deadline;

    /** 是否允许迟交：0否 1是 */
    private Integer allowLateSubmit;

    private LocalDateTime publishTime;

    /** 作业状态：2=已发布，3=已截止 */
    private Integer status;

    /** 作业状态名 */
    private String statusName;

    /** 是否已提交过（至少提交过一题） */
    private Boolean submitted;

    /** 已提交题目数 */
    private Integer submittedQuestionCount;

    /** 最近一次提交时间 */
    private LocalDateTime lastSubmitTime;

    /**
     * 批改状态：0=待批改，1=已完成，2=批改失败；
     * 未提交或未生成批改任务时为 null
     */
    private Integer gradingStatus;

    /** 批改状态名，未批改时为空串 */
    private String gradingStatusName;

    /** 批改完成后学生得分；未完成时为 null */
    private Integer studentScore;

    /** 是否已过截止时间 */
    private Boolean expired;

    /** 当前是否仍可提交（已截止且不允许迟交、或作业已关闭时为 false） */
    private Boolean canSubmit;
}
