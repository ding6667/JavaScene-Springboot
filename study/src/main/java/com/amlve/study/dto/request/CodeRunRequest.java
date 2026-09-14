package com.amlve.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 代码运行请求（学生端提交）
 * 注意：userId 由后端从 Token 中解析，前端不传
 */
@Data
public class CodeRunRequest {

    /**
     * 学生提交的代码
     */
    @NotBlank(message = "代码内容不能为空")
    private String codeContent;
    /**
     * 关联章节ID（自由练习时必填）
     */
    private Long chapterId;

    /**
     * 关联题目ID（做具体题目时必填，自由练习时为 null）
     */
    private Long questionId;
}