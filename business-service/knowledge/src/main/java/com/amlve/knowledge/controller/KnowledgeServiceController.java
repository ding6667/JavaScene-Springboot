package com.amlve.knowledge.controller;

import com.amlve.common.request.Result;
import com.amlve.knowledge.service.impl.KnowledgeDatasetServiceImpl;
import com.amlve.knowledgeapi.dto.response.KnowledgeResponse;
import com.amlve.knowledgeapi.dto.response.WeightConfigVO;
import com.amlve.knowledgeapi.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * KnowledgeService 的 HTTP 提供端：Feign 客户端按同一接口调用，方法映射继承自接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/knowledge/service")
public class KnowledgeServiceController implements KnowledgeService {

    private final KnowledgeDatasetServiceImpl knowledgeService;

    @Override
    public Result<KnowledgeResponse> queryKnowledge(String keywords) {
        return knowledgeService.queryKnowledge(keywords);
    }

    @Override
    public Result<List<WeightConfigVO>> getWeightConfigsByChapter(Long chapterId) {
        return knowledgeService.getWeightConfigsByChapter(chapterId);
    }
}
