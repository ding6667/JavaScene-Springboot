package com.amlve.teach.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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

    /** 作业总分，默认 0；传了题目时由题目分值汇总覆盖 */
    @Min(value = 0, message = "作业总分不能为负数")
    private Integer totalScore = 0;

    /**
     * 发布时选中的题目，数组顺序即题目顺序（sortOrder 从 1 开始）。
     * 不传或空数组表示只建作业、暂不挂题。
     */
    @Size(max = 100, message = "单次最多发布100道题")
    @Valid
    private List<QuestionItem> questions;

    /** 选中题目：题目ID + 本题分值（可选）+ 来源（可选） */
    @Data
    public static class QuestionItem {

        @NotNull(message = "题目ID不能为空")
        private Long questionId;

        /** 本题分值；不传则用题库中该题的默认分值 */
        @Min(value = 0, message = "题目分值不能为负数")
        private Integer score;

        /** 来源：1教材习题生成 2AI生成 3人工录入，默认 3（教师从题库手选） */
        @Min(value = 1, message = "题目来源只能为1、2、3")
        @Max(value = 3, message = "题目来源只能为1、2、3")
        private Integer sourceType;
    }
}
