package com.amlve.examapi.dto.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 错题归档请求（跨模块：study 判分错题时调用 exam-api 写入错题本）。
 * <p>
 * 归档是幂等的：同一学生、同一题目快照、同一来源第二次进来只累计 wrongCount 并刷新最近答案，
 * 不会重复插入。题面由 exam 侧按快照生成，调用方不需要传题目内容。
 */
@Data
public class WrongQuestionArchiveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 题库题目ID（exam_question.id），不是作业题目快照ID
     */
    private Long questionId;

    /**
     * 最近一次错误答案（学生作答原文，可空；超长时由调用方截断）
     */
    private String lastAnswer;

    /**
     * 来源：1=作业，2=刷题；不传按 1（作业）处理
     */
    private Integer source;

    /**
     * 作业题目快照ID（teach_homework_question.id）。
     * 作业批改时传入，错题会钉在「发布的那一版题」上，题库后续修改不影响。
     */
    private Long homeworkSnapshotId;

    /**
     * 题目快照ID（exam_question_snapshot.id）。
     * 错题重做时传入，保证重做记录仍挂在原来那一版快照上，而不是新开一版。
     */
    private Long snapshotId;
}
