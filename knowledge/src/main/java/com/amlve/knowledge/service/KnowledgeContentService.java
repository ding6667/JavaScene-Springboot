package com.amlve.knowledge.service;

import com.amlve.knowledgeapi.dto.request.KnowledgeContentSaveRequest;

/**
 * 教材原文维护服务（写侧）
 * <p>
 * 查询侧不在本接口声明：出参统一由 knowledge-api 的 KnowledgeContentApi 提供
 * （返回 Result 包装），本模块控制器也直接注入它，避免同一方法在两个接口里
 * 因返回类型不同而签名冲突。
 */
public interface KnowledgeContentService {

    /**
     * 新增或更新教材原文，并同步为 Dify 知识库文档
     *
     * @param request 内容（id 为空则新增）
     * @return 记录ID
     */
    Long saveContent(KnowledgeContentSaveRequest request);

    /**
     * 逻辑删除教材原文，并归档删除 Dify 侧文档
     *
     * @param id 记录ID
     */
    void deleteContent(Long id);
}
