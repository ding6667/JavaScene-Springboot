package com.amlve.exam.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 出题请求（按章节随机抽取）
 */
@Data
public class QuestionGenerateRequest {

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    @NotNull(message = "抽题数量不能为空")
    private Integer count;

    private Integer type;

    private Integer difficulty;
}