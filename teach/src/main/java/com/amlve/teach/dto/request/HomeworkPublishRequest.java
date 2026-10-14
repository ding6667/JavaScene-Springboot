package com.amlve.teach.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师发布作业的请求参数（前端提交用）。
 * 发布范围：classId（指定班级）与 grade（全年级）二选一，校验在 Service 层执行。
 */
@Data
public class HomeworkPublishRequest {

    @NotBlank(message = "作业标题不能为空")
    @Size(max = 100, message = "作业标题不能超过100字")
    private String title;

    @NotBlank(message = "作业内容不能为空")
    private String content;

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    @NotNull(message = "截止时间不能为空")
    private LocalDateTime deadline;

    /** 发布班级ID（指定班级发布时必填） */
    private Long classId;

    /** 年级（全年级发布时必填） */
    @Size(max = 20, message = "年级不能超过20字")
    private String grade;

    /** 是否允许迟交：0否 1是，默认 0 */
    @Min(value = 0, message = "是否允许迟交只能为0或1")
    @Max(value = 1, message = "是否允许迟交只能为0或1")
    private Integer allowLateSubmit = 0;

    /** 作业总分，默认 0 */
    @Min(value = 0, message = "作业总分不能为负数")
    private Integer totalScore = 0;
}
