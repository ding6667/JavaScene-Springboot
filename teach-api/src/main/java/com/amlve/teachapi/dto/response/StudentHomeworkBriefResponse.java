package com.amlve.teachapi.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

import java.io.Serializable;
@Data
public class StudentHomeworkBriefResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long homeworkId;
    private String title;
    private String content;
    private Long chapterId;
    private String chapterName;
    private Integer questionCount;
    private Integer totalScore;
    private LocalDateTime deadline;
    private Integer allowLateSubmit;
    private LocalDateTime publishTime;
}
