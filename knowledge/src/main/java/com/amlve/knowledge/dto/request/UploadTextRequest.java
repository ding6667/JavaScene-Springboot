package com.amlve.knowledge.dto.request;

import com.amlve.knowledge.constant.ErrorMessageConstant;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 知识库文本文档上传请求（运维用）
 */
@Data
public class UploadTextRequest {

    /**
     * 关联章节ID
     */
    @NotNull(message = ErrorMessageConstant.CHAPTER_ID_EMPTY)
    private Long chapterId;

    /**
     * 文档名称
     */
    @NotBlank(message = ErrorMessageConstant.DOCUMENT_NAME_EMPTY)
    private String documentName;

    /**
     * 教材原文内容
     */
    @NotBlank(message = ErrorMessageConstant.CONTENT_EMPTY)
    private String content;

    /**
     * 内容类型（1=概念定义，2=例题，3=代码案例，4=习题，可为空）
     */
    private Integer contentType;

    /**
     * 教材页码（新版表结构为 int，按数值序排序）
     */
    @Min(value = 0, message = "页码不能为负数")
    private Integer page;
}
