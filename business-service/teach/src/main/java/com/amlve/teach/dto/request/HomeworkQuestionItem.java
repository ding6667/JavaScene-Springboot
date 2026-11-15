package com.amlve.teach.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发布作业时选择的题目条目（教师指定每题分值与排序）
 */
@Data
public class HomeworkQuestionItem {

    /** 题目ID（关联 exam_question） */
    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    /** 本题分值，可选；为空时学生端展示使用题目默认分值 */
    @Min(value = 1, message = "题目分值必须为正数")
    private Integer score;

    /** 题目排序，可选；为空时按请求列表顺序 */
    @Min(value = 0, message = "题目排序不能为负数")
    private Integer sortOrder;
}
