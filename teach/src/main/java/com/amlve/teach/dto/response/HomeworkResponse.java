package com.amlve.teach.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 返回给前端的作业信息
 */
@Data
public class HomeworkResponse {

    private Long id;

    private String title;

    private String content;

    private Long chapterId;

    private LocalDateTime deadline;

    private LocalDateTime createTime;
}
