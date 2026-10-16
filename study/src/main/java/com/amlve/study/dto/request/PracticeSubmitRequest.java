package com.amlve.study.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;



@Data
public class PracticeSubmitRequest {

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    /**
     * 学生答案（代码或选项）
     */
    private String answerContent;

    /**
     * 本题耗时（秒），由前端累加计算后传入
     */
    private Integer duration;

    /**
     * 尝试次数
     */
    private Integer attemptCount;

    /**
     * 练习模式：1=随机，2=章节专项，3=错题重做
     */
    private Integer practiceMode;
}