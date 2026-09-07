package com.amlve.knowledge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库文档上传响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    /**
     * Dify 文档ID（异步索引中，稍后可检索）
     */
    private String documentId;
}
