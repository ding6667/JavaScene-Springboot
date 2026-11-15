package com.amlve.knowledge.service;

import com.amlve.knowledge.dto.request.UpdateMetadataRequest;
import com.amlve.knowledge.dto.request.UploadTextRequest;
import com.amlve.knowledge.dto.response.KnowledgeResponse;
import com.amlve.knowledge.dto.response.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 知识库数据集服务接口（检索/上传/元数据/校验）
 */
public interface KnowledgeDatasetService {

    /**
     * 根据关键词从 Dify 知识库检索相关知识
     */
    KnowledgeResponse query(String keywords);

    /**
     * 上传文本文档（异步）：校验通过后建立"处理中"留痕并把建档任务投入后台线程，
     * 立即返回 recordId；建档→写元数据→落库 knowledge_content→回写留痕终态在后台完成
     */
    UploadResponse uploadText(UploadTextRequest request);

    /**
     * 上传文件文档（异步）：口径同 {@link #uploadText}，暂存落盘在请求线程内完成
     */
    UploadResponse uploadFile(Long chapterId, MultipartFile file);

    /**
     * 更新 Dify 文档元数据（运维用）
     */
    String updateMetadata(UpdateMetadataRequest request);
}
