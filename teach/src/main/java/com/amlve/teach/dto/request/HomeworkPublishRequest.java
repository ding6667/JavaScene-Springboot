package com.amlve.teach.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师发布作业的请求参数（前端提交用）
 */
@Data
public class HomeworkPublishRequest {

    @NotBlank(message = "作业标题不能为空")
    private String title;

    @NotBlank(message = "作业内容不能为空")
    private String content;

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    @NotNull(message = "截止时间不能为空")
    private LocalDateTime deadline;
}
