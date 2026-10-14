package com.amlve.agentqaapi.dto.response;

import lombok.Data;

/**
 * Dify 建档结果（文档ID + 批次 + 索引分段数）
 * <p>
 * 建档接口原本只回传 documentId，导致调用方无法记录"生成了多少分段"这一
 * 空壳判定依据（total_segments=0 即空壳）。本对象把 batch 与分段数一并透出，
 * 供知识库上传留痕（knowledge_upload_record）写入。
 */
@Data
public class DifyDocumentVO {

    /**
     * Dify 文档ID
     */
    private String documentId;

    /**
     * Dify 批次标识，用于查询索引状态
     */
    private String batch;

    /**
     * 索引完成后生成的分段总数；0 表示空壳文档
     */
    private Integer totalSegments;

    /**
     * 已索引完成的分段数（Dify 原样回传，用于留痕区分"部分完成"与"全部完成"）
     */
    private Integer completedSegments;

    public DifyDocumentVO() {
    }

    public DifyDocumentVO(String documentId, String batch, Integer totalSegments) {
        this(documentId, batch, totalSegments, totalSegments);
    }

    public DifyDocumentVO(String documentId, String batch, Integer totalSegments, Integer completedSegments) {
        this.documentId = documentId;
        this.batch = batch;
        this.totalSegments = totalSegments;
        this.completedSegments = completedSegments;
    }
}
