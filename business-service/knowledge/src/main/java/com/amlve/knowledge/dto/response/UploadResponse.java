package com.amlve.knowledge.dto.response;

import com.amlve.knowledge.enumeration.UploadStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库文档上传受理响应
 * <p>
 * 上传已改为异步：接口只做参数与文件类型校验、建立留痕记录并把建档任务投入后台线程，
 * 因此返回体里<b>没有</b> documentId / batch / 分段数——这些要等 Dify 索引完成才存在。
 * 前端凭 {@link #recordId} 轮询 {@code GET /api/knowledge/dataset/upload/list} 取最终状态
 * （status 0=处理中，1=成功，2=失败，失败原因在列表的 errorMsg 里）。
 * <p>
 * success 表示"受理成功"（校验通过且任务已入队），不代表知识库真的有内容；
 * 真正的成败以留痕记录的 status 为准，避免重演九月"接口成功但内容为空"的假成功。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    /**
     * 是否受理成功
     */
    private Boolean success;

    /**
     * 上传记录ID（knowledge_upload_record.id），前端据此查询最终状态
     */
    private Long recordId;

    /**
     * 受理时的状态码，恒为 0（处理中）
     */
    private Integer status;

    /**
     * 状态中文名
     */
    private String statusDesc;

    /**
     * 失败原因（受理成功时为 null，如队列已满）
     */
    private String errorMsg;

    /**
     * 构造一个受理成功响应
     *
     * @param recordId 上传记录ID
     * @return 处理中响应
     */
    public static UploadResponse accepted(Long recordId) {
        UploadResponse response = new UploadResponse();
        response.setSuccess(true);
        response.setRecordId(recordId);
        response.setStatus(UploadStatusEnum.PROCESSING.getCode());
        response.setStatusDesc(UploadStatusEnum.PROCESSING.getDesc());
        return response;
    }
}
