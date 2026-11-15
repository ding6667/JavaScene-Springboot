package com.amlve.knowledge.service;

import com.amlve.knowledgeapi.dto.request.KnowledgePointSaveRequest;

/**
 * 知识点维护服务
 * <p>
 * 查询侧复用 knowledge-api 的 KnowledgeChapterApi.listPointsByChapter，此处只负责写入。
 */
public interface KnowledgePointService {

    /**
     * 新增或更新知识点（id 为空则新增）
     *
     * @param request 知识点内容
     * @return 知识点ID
     */
    Long savePoint(KnowledgePointSaveRequest request);
}
