package com.amlve.knowledge.mapper;

import com.amlve.knowledge.tableEnt.KnowledgeWeightConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 知识点权重配置 Mapper
 * <p>
 * 继承 BaseMapper 即获得通用 CRUD，无需手写 SQL；
 * 复杂查询（如关联知识点名称）在 Service 层组装。
 */
public interface KnowledgeWeightConfigMapper extends BaseMapper<KnowledgeWeightConfig> {
}
