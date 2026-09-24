package com.amlve.exam.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 作业提交请求（学生按题提交答案）
 */
@Data
public class HomeworkSubmitRequest {

    @NotNull(message = "作业ID不能为空")
    private Long homeworkId;

    @NotEmpty(message = "答案列表不能为空")
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        @NotNull(message = "题目ID不能为空")
        private Long questionId;

        private String answerContent;
    }
}