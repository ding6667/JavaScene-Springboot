package com.amlve.knowledge.service;

import com.amlve.knowledge.dto.request.WeightConfigSaveRequest;
import com.amlve.knowledgeapi.dto.response.WeightConfigVO;

import java.util.List;

/**
 * 知识点权重配置服务接口（管理员维护出题权重策略）
 */
public interface KnowledgeWeightConfigService {

    /**
     * 新增/更新单条权重配置（管理员用）
     * <p>
     * 按"章节ID + 知识点ID"定位：已存在则更新，不存在则插入（upsert，
     * 对应数据库唯一键 uk_chapter_point）。
     *
     * @param request 保存请求（字段校验由 Controller 层 @Valid 完成）
     * @return 配置ID（新增为新生成的ID，更新为原ID）
     */
    Long saveWeightConfig(WeightConfigSaveRequest request);

    /**
     * 批量保存权重配置（管理员用）
     * <p>
     * 逐条走 upsert 逻辑，整体在同一事务内：任一条失败全部回滚。
     *
     * @param requests 批量保存请求列表
     * @return 实际处理的条数
     */
    int batchSaveWeightConfigs(List<WeightConfigSaveRequest> requests);

    /**
     * 根据章节ID查询该章节全部权重配置（关联知识点名称）
     * <p>
     * 供管理端展示；无配置时返回空列表。
     *
     * @param chapterId 章节ID
     * @return 权重配置列表（含知识点名称）
     */
    List<WeightConfigVO> listByChapter(Long chapterId);
}
