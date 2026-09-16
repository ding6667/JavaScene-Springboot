package com.amlve.systemapi.service;

import com.amlve.systemapi.dto.api.StandardConfigDTO;

/**
 * 评分标准配置对外服务接口（供其他模块调用，如 exam 批阅）
 * <p>
 * 跨模块只允许依赖 system-api 的接口，禁止注入 system 实现类。
 */
public interface StandardService {

    /**
     * 按标准名称查询评分标准
     *
     * @param name 标准名称
     * @return 评分标准配置，不存在时返回 null
     */
    StandardConfigDTO selectByName(String name);

    /**
     * 按ID查询评分标准
     *
     * @param id 标准ID
     * @return 评分标准配置，不存在时返回 null
     */
    StandardConfigDTO selectById(Long id);
}
