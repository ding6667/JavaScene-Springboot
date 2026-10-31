package com.amlve.exam.service;

import com.amlve.exam.dto.response.DifficultyConfigResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 出题难度配置业务接口：题库 difficulty 已改为指向本表的逻辑外键，
 * 因此难度名称的展示、抽题入参校验、AI 出题结果的回落都从这里取。
 */
public interface ExamDifficultyConfigService {

    /**
     * 查询当前用户可用的难度配置（启用中，系统预设或本人自建；按章节过滤时含通用配置）。
     *
     * @param chapterId 章节ID，为空时只返回通用配置
     * @return 按难度从易到难排序的配置列表
     */
    List<DifficultyConfigResponse> listUsable(Long chapterId);

    /**
     * 批量取难度名称，供列表类接口一次性填好 VO（含已停用的历史引用，避免旧题显示空白）。
     *
     * @param ids 配置ID集合，可含 null 与重复值
     * @return id → 名称；入参为空时返回空 Map，已删除的配置不出现在结果里
     */
    Map<Long, String> namesByIds(Collection<Long> ids);

    /**
     * 单个配置ID取名称。
     *
     * @param id 配置ID，可空
     * @return 难度名称；ID 为空或配置已被删除时返回 null
     */
    String nameOf(Long id);

    /**
     * 按难度编码查启用中的系统预设配置ID（AI 出题返回 basic/intermediate/advanced 时回落用）。
     *
     * @param code 难度编码
     * @return 配置ID；无匹配时返回 null
     */
    Long idByCode(String code);
}
