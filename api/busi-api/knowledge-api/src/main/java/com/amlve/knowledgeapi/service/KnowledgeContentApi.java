package com.amlve.knowledgeapi.service;

import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.knowledgeapi.dto.request.KnowledgeContentQueryRequest;
import com.amlve.knowledgeapi.dto.response.KnowledgeContentVO;
import com.amlve.knowledgeapi.dto.response.KnowledgePointDTO;

import java.util.List;

/**
 * 教材原文与知识点对外接口（供备课、出题、批改智能体调用）
 * <p>
 * 跨模块只允许依赖 knowledge-api 的接口，禁止注入 knowledge 实现类。
 */
public interface KnowledgeContentApi {

    /**
     * 按章节分页查询教材原文素材（按 page、pageOrder 升序）
     *
     * @param request 查询条件（章节必填，类型与关键词可选）
     * @return 分页素材列表
     */
    Result<PageResult<KnowledgeContentVO>> listContents(KnowledgeContentQueryRequest request);

    /**
     * 按章节取素材，单次最多返回 200 条
     * <p>
     * 备课智能体一次要拉整章素材，分页参数对它没意义，故提供不分页版本；
     * 超过 200 条直接返回参数错误，避免一次拖走整章数据却只用到前几条。
     *
     * @param chapterId   章节ID
     * @param contentType 内容类型（可为空表示不限）
     * @return 素材列表
     */
    Result<List<KnowledgeContentVO>> listContentsByChapter(Long chapterId, Integer contentType);

    /**
     * 按ID查询知识点
     *
     * @param pointId 知识点ID
     * @return 知识点；不存在时 data 为 null（不报错，由调用方判断）
     */
    Result<KnowledgePointDTO> getKnowledgePointById(Long pointId);
}
