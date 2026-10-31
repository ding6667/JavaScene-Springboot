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

    /** 作业状态：2=已发布，3=已截止（学生可见范围只有这两种） */
    private Integer status;

    /** 作业状态名，未匹配时为空串 */
    private String statusName;
}
