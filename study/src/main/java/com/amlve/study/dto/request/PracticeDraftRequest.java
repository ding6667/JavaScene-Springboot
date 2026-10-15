package com.amlve.study.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 刷题草稿保存请求（任务书 3.4）
 *
 * <p>前端代码变动触发（需节流：输入停顿 3s 或每 30s 一次），接口幂等可重复调用。
 */
@Data
public class PracticeDraftRequest {

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    /**
     * 学生当前代码/答案（允许为空：清空代码场景，此时不覆盖已有答案）
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
}
