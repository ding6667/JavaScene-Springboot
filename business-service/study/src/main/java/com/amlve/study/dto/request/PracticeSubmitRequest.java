package com.amlve.study.dto.request;

import lombok.Data;


@Data
public class PracticeSubmitRequest {

    /**
     * 题目ID（题库ID）。
     * <p>
     * 错题重做时可以只传 {@link #wrongQuestionId}，后端按错题记录回填题目ID。
     */
    private Long questionId;

    /**
     * 错题记录ID（exam_wrong_question.id）。
     * <p>
     * 传了它就是「错题重做」：按该错题记录冻结的题目快照判分，
     * 不再读题库最新版，也不会用新测试用例去判旧代码。
     */
    private Long wrongQuestionId;

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
