package com.amlve.exam.dto.request;

import com.amlve.exam.tableEnt.ExamQuestion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 题目新增/更新请求
 */
@Data
public class QuestionSaveRequest {

    @NotBlank(message = "题目内容不能为空")
    private String content;

    @NotNull(message = "题型不能为空")
    private Integer type;

    private List<String> options;

    @NotNull(message = "标准答案不能为空")
    private ExamQuestion.QuestionAnswer answer;

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    private Integer difficulty;

    private Integer score;

    private String codeFramework;

    private String cognitiveLevel;
}
