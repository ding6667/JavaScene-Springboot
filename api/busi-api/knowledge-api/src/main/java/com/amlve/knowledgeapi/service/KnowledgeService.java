package com.amlve.knowledgeapi.service;

import com.amlve.common.request.Result;
import com.amlve.common.web.feign.FeignConfig;
import com.amlve.knowledgeapi.dto.response.KnowledgeResponse;
import com.amlve.knowledgeapi.dto.response.WeightConfigVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 知识库对外服务接口（供其他模块调用，如 study 答疑）
 */
@FeignClient(name = "knowledge-service", contextId = "knowledgeService", path = "/internal/knowledge/service", configuration = FeignConfig.class)
public interface KnowledgeService {

    /**
     * 根据关键词从 Dify 知识库检索相关知识
     *
     * @param keywords 检索关键词
     * @return 检索结果（未命中时 message 为"未找到相关知识"）
     */
    @PostMapping("/queryKnowledge")
    Result<KnowledgeResponse> queryKnowledge(@RequestParam("keywords") String keywords);


    /**
     * 根据章节ID获取该章节所有知识点权重配置
     * 用于出题智能体计算各知识点出题数量
     */
    @PostMapping("/getWeightConfigsByChapter")
    Result<List<WeightConfigVO>> getWeightConfigsByChapter(@RequestParam("chapterId") Long chapterId);


}
