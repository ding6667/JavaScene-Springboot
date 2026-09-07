package com.amlve.knowledge.dto.request;

import com.amlve.knowledge.constant.ErrorMessageConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

/**
 * 知识库文档元数据更新请求（运维用）
 */
@Data
public class UpdateMetadataRequest {

    /**
     * Dify 文档ID
     */
    @NotBlank(message = ErrorMessageConstant.DOCUMENT_ID_EMPTY)
    private String documentId;

    /**
     * 元数据键值对（如 chapter_id=3, page=P.123）
     */
    @NotEmpty(message = ErrorMessageConstant.METADATA_EMPTY)
    private Map<String, Object> metadata;
}
