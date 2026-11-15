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

    /** 作业 ID */
    @NotNull(message = "作业ID不能为空")
    private Long homeworkId;

    /** 本题答案列表 */
    @NotEmpty(message = "答案列表不能为空")
    private List<AnswerItem> answers;

    /** 单题作答 */
    @Data
    public static class AnswerItem {
        /** 题目 ID */
        @NotNull(message = "题目ID不能为空")
        private Long questionId;

        /**
         * 作业题目快照ID（{@code /student/question} 返回的 {@code id}）。
         * <p>
         * 不传也能提交：后端会按 homeworkId + questionId 回查快照补上；
         * 传了则以传入为准，用于批改时把错题钉在发布那一版题上。
         */
        private Long snapshotId;

        /** 学生作答内容 */
        private String answerContent;
    }
}