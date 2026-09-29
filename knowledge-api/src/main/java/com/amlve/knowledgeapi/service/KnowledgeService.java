package com.amlve.knowledgeapi.service;

import com.amlve.common.request.Result;
import com.amlve.knowledgeapi.dto.response.KnowledgeResponse;
import com.amlve.knowledgeapi.dto.response.WeightConfigVO;

import java.util.List;

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
    Result<KnowledgeResponse> queryKnowledge(String keywords);


    /**
     * 根据章节ID获取该章节所有知识点权重配置
     * 用于出题智能体计算各知识点出题数量
     */
    Result<List<WeightConfigVO>> getWeightConfigsByChapter(Long chapterId);


}
