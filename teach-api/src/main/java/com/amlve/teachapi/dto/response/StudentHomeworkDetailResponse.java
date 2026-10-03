package com.amlve.teachapi.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生作业详情（teach → study / exam）
 * 含作业元信息 + 题目快照（去答案）
 */
@Data
public class StudentHomeworkDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long homeworkId;
    private String title;
    private String content;
    private Long chapterId;
    private String chapterName;
    private Integer totalScore;
    private Integer questionCount;
    private LocalDateTime deadline;
    private Integer allowLateSubmit;
    private LocalDateTime publishTime;
    private List<StudentQuestionItem> questions;

    @Data
    public static class StudentQuestionItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 作业题目ID（teach_homework_question.id），提交时用它 */
        private Long id;

        private String content;
        private Integer type;
        private String typeName;
        private List<OptionItem> options;
        private Integer score;
        private Integer sortOrder;
    }

    @Data
    public static class OptionItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String key;
        private String value;
    }
}