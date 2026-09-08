package com.amlve.knowledgeapi.service;

import com.amlve.knowledgeapi.dto.response.KnowledgeResponse;

/**
 * 知识库对外服务接口（供其他模块调用，如 study 答疑）
 */
public interface KnowledgeService {

    /**
     * 根据关键词从 Dify 知识库检索相关知识
     *
     * @param keywords 检索关键词
     * @return 检索结果（未命中时 message 为"未找到相关知识"）
     */
    KnowledgeResponse queryKnowledge(String keywords);
}
