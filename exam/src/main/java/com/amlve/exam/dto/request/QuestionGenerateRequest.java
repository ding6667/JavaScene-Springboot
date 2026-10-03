package com.amlve.exam.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 出题请求（按章节随机抽取）
 */
@Data
public class QuestionGenerateRequest {

    /** 章节 ID */
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    /** 抽题数量 */
    @NotNull(message = "抽题数量不能为空")
    private Integer count;

    /** 题型（可选，1=选择 2=填空 3=判断 4=程序阅读 5=编程） */
    private Integer type;

    /** 难度（可选，1=基础 2=进阶 3=拔高） */
    private Integer difficulty;
}