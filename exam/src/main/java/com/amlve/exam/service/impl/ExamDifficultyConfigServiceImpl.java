package com.amlve.exam.service.impl;

import com.amlve.common.utils.SecurityUtils;
import com.amlve.exam.dto.response.DifficultyConfigResponse;
import com.amlve.exam.mapper.ExamQuestionDifficultyConfigMapper;
import com.amlve.exam.service.ExamDifficultyConfigService;
import com.amlve.exam.tableEnt.ExamQuestionDifficultyConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 出题难度配置业务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamDifficultyConfigServiceImpl implements ExamDifficultyConfigService {

    private static final int STATUS_ENABLED = 1;
    private static final int SYSTEM_PRESET = 1;

    private final ExamQuestionDifficultyConfigMapper difficultyConfigMapper;

    @Override
    public List<DifficultyConfigResponse> listUsable(Long chapterId) {
        Long teacherId = SecurityUtils.getUserId();
        LambdaQueryWrapper<ExamQuestionDifficultyConfig> wrapper = Wrappers.<ExamQuestionDifficultyConfig>lambdaQuery()
                .eq(ExamQuestionDifficultyConfig::getStatus, STATUS_ENABLED)
                // 系统预设 + 本人自建，他人自定义配置不可见
                .and(w -> w.eq(ExamQuestionDifficultyConfig::getIsSystem, SYSTEM_PRESET)
                        .or().eq(ExamQuestionDifficultyConfig::getTeacherId, teacherId))
                .orderByAsc(ExamQuestionDifficultyConfig::getLevelOrder)
                .orderByAsc(ExamQuestionDifficultyConfig::getSortOrder);
        if (chapterId == null) {
            wrapper.isNull(ExamQuestionDifficultyConfig::getChapterId);
        } else {
            // 指定章节时通用配置（chapter_id 为 NULL）依然可选
            wrapper.and(w -> w.isNull(ExamQuestionDifficultyConfig::getChapterId)
                    .or().eq(ExamQuestionDifficultyConfig::getChapterId, chapterId));
        }
        return difficultyConfigMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public Map<Long, String> namesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Set<Long> distinct = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return difficultyConfigMapper.selectBatchIds(distinct).stream()
                .filter(config -> config.getName() != null)
                .collect(Collectors.toMap(ExamQuestionDifficultyConfig::getId,
                        ExamQuestionDifficultyConfig::getName, (a, b) -> a));
    }

    @Override
    public String nameOf(Long id) {
        if (id == null) {
            return null;
        }
        ExamQuestionDifficultyConfig config = difficultyConfigMapper.selectById(id);
        return config == null ? null : config.getName();
    }

    @Override
    public Long idByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        List<ExamQuestionDifficultyConfig> matched = difficultyConfigMapper.selectList(
                Wrappers.<ExamQuestionDifficultyConfig>lambdaQuery()
                        .eq(ExamQuestionDifficultyConfig::getCode, code.trim())
                        .eq(ExamQuestionDifficultyConfig::getStatus, STATUS_ENABLED)
                        .eq(ExamQuestionDifficultyConfig::getIsSystem, SYSTEM_PRESET)
                        .isNull(ExamQuestionDifficultyConfig::getChapterId)
                        .orderByAsc(ExamQuestionDifficultyConfig::getLevelOrder));
        return matched.isEmpty() ? null : matched.get(0).getId();
    }

    private DifficultyConfigResponse toVO(ExamQuestionDifficultyConfig config) {
        DifficultyConfigResponse vo = new DifficultyConfigResponse();
        BeanUtils.copyProperties(config, vo);
        return vo;
    }
}
