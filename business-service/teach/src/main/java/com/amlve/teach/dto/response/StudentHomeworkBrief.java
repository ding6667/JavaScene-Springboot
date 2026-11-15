package com.amlve.teach.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生端作业简短信息（作业列表/作答页头部用）。
 * <p>
 * 只带作业元信息与题目快照 ID 列表，不带题干/答案：
 * 前端拿到 {@code questionIds} 后逐题调 {@code /student/question} 懒加载，避免一次拉全量题干。
 * 本接口会校验作业对当前学生可见：状态为已发布/已截止，且班级或年级在发布范围内。
 */
@Data
public class StudentHomeworkBrief {

    private Long homeworkId;

    private String title;

    /** 作业要求/说明 */
    private String content;

    private Long chapterId;

    /** 章节全称（如「第 2 章 Java 编程基础」）；章节已删或知识模块未就绪时为 null */
    private String chapterName;

    /** 题目数量（以题目快照实际条数为准，可能与发布时统计值有出入） */
    private Integer questionCount;

    private Integer totalScore;

    private LocalDateTime deadline;

    /** 是否允许迟交：0否 1是 */
    private Integer allowLateSubmit;

    private LocalDateTime publishTime;

    /** 作业状态：2=已发布，3=已截止 */
    private Integer status;

    private String statusName;

    /**
     * 题目快照ID列表（teach_homework_question.id），按 sortOrder 升序；
     * 逐题详情用 {@code GET /api/teach/homework/student/question?questionId=} 获取。
     */
    private List<Long> questionIds;
}
