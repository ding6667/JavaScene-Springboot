package com.amlve.knowledge.service;

import com.amlve.knowledge.dto.request.UpdateMetadataRequest;
import com.amlve.knowledge.dto.request.UploadTextRequest;
import com.amlve.knowledge.dto.response.KnowledgeResponse;
import com.amlve.knowledge.dto.response.UploadResponse;
import com.amlve.knowledge.dto.response.VerifyResponse;
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
     * 上传文本文档：Dify 创建文档 + 写入元数据 + 落库 knowledge_content（事务）
     */
    UploadResponse uploadText(UploadTextRequest request);

    /**
     * 上传文件文档：Dify 创建文档 + 写入元数据 + 落库 knowledge_content（事务）
     */
    UploadResponse uploadFile(Long chapterId, MultipartFile file);

    /**
     * 更新 Dify 文档元数据（运维用）
     */
    String updateMetadata(UpdateMetadataRequest request);

    /**
     * AI 内容校验：判断内容是否包含教材关键词
     */
    VerifyResponse verifyContent(String content);
}
