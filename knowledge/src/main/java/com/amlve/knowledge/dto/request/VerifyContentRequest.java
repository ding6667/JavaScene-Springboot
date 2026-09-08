package com.amlve.knowledge.dto.request;

import com.amlve.knowledge.constant.ErrorMessageConstant;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 内容校验请求
 */
@Data
public class VerifyContentRequest {

    /**
     * 待校验的文本内容
     */
    @NotBlank(message = ErrorMessageConstant.VERIFY_CONTENT_EMPTY)
    private String content;
}
