package com.amlve.knowledge.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.knowledge.constant.ErrorMessageConstant;
import com.amlve.knowledge.dto.request.WeightConfigSaveRequest;
import com.amlve.knowledge.mapper.KnowledgePointMapper;
import com.amlve.knowledge.mapper.KnowledgeWeightConfigMapper;
import com.amlve.knowledge.service.KnowledgeWeightConfigService;
import com.amlve.knowledge.tableEnt.KnowledgePoint;
import com.amlve.knowledge.tableEnt.KnowledgeWeightConfig;
import com.amlve.knowledgeapi.dto.response.WeightConfigVO;
import com.amlve.systemapi.service.UserService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 知识点权重配置服务实现
 * <p>
 * 写操作（保存/批量保存）仅限管理员：JWT 令牌无角色信息，
 * 通过 system-api 的 {@code UserService.isAdmin} 编程式校验
 * （与 KnowledgeDatasetServiceImpl 的 requireTeacher 同款模式）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeWeightConfigServiceImpl implements KnowledgeWeightConfigService {

    private final KnowledgeWeightConfigMapper weightConfigMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final UserService userService;

    @Override
    public Long saveWeightConfig(WeightConfigSaveRequest request) {
        requireAdmin();
        checkBusinessRule(request);
        KnowledgeWeightConfig config = upsert(request);
        log.info("保存权重配置: id={}, chapterId={}, knowledgePointId={}, weight={}",
                config.getId(), request.getChapterId(), request.getKnowledgePointId(), request.getWeight());
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSaveWeightConfigs(List<WeightConfigSaveRequest> requests) {
        requireAdmin();
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(ErrorMessageConstant.WEIGHT_BATCH_EMPTY);
        }
        int count = 0;
        for (WeightConfigSaveRequest request : requests) {
            checkBusinessRule(request);
            upsert(request);
            count++;
        }
        log.info("批量保存权重配置完成: 共 {} 条", count);
        return count;
    }

    @Override
    public List<WeightConfigVO> listByChapter(Long chapterId) {
        if (ObjectUtils.isEmpty(chapterId)) {
            throw new BusinessException(ErrorMessageConstant.CHAPTER_ID_EMPTY);
        }
        return assembleVos(weightConfigMapper.selectList(
                Wrappers.lambdaQuery(KnowledgeWeightConfig.class)
                        .eq(KnowledgeWeightConfig::getChapterId, chapterId)
                        .orderByDesc(KnowledgeWeightConfig::getWeight)));
    }

    /**
     * 按"章节+知识点"定位：存在则更新，不存在则插入（对应唯一键 uk_chapter_point）
     */
    private KnowledgeWeightConfig upsert(WeightConfigSaveRequest request) {
        KnowledgeWeightConfig exist = weightConfigMapper.selectOne(
                Wrappers.lambdaQuery(KnowledgeWeightConfig.class)
                        .eq(KnowledgeWeightConfig::getChapterId, request.getChapterId())
                        .eq(KnowledgeWeightConfig::getKnowledgePointId, request.getKnowledgePointId())
                        .last("LIMIT 1"));
        KnowledgeWeightConfig config;
        if (exist != null) {
            config = exist;
        } else {
            config = new KnowledgeWeightConfig();
            config.setChapterId(request.getChapterId());
            config.setKnowledgePointId(request.getKnowledgePointId());
        }
        config.setWeight(request.getWeight());
        config.setBaseQuestionCount(request.getBaseQuestionCount());
        config.setMaxQuestionCount(request.getMaxQuestionCount());
        config.setDifficultyDistribution(request.getDifficultyDistribution());
        // isActive 可空，默认启用
        config.setIsActive(Boolean.FALSE.equals(request.getIsActive()) ? 0 : 1);
        if (exist != null) {
            weightConfigMapper.updateById(config);
        } else {
            weightConfigMapper.insert(config);
        }
        return config;
    }

    /**
     * 业务规则校验（跨字段约束，注解表达不了的部分）：
     * 1. 章节必须存在；2. 知识点必须存在且属于该章节；3. 最大出题数 >= 基础出题数
     */
    private void checkBusinessRule(WeightConfigSaveRequest request) {
        KnowledgePoint point = knowledgePointMapper.selectById(request.getKnowledgePointId());
        if (point == null) {
            throw new BusinessException(ErrorMessageConstant.WEIGHT_POINT_NOT_FOUND);
        }
        if (!request.getChapterId().equals(point.getChapterId())) {
            throw new BusinessException(ErrorMessageConstant.WEIGHT_POINT_CHAPTER_MISMATCH);
        }
        if (request.getMaxQuestionCount() < request.getBaseQuestionCount()) {
            throw new BusinessException(ErrorMessageConstant.WEIGHT_MAX_LESS_THAN_BASE);
        }
    }

    /**
     * 配置记录组装为 VO：一次批量查询知识点名称，避免 N+1 查询
     */
    private List<WeightConfigVO> assembleVos(List<KnowledgeWeightConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> pointIds = configs.stream()
                .map(KnowledgeWeightConfig::getKnowledgePointId)
                .distinct()
                .toList();
        Map<Long, KnowledgePoint> pointMap = knowledgePointMapper.selectBatchIds(pointIds).stream()
                .collect(Collectors.toMap(KnowledgePoint::getId, Function.identity()));
        List<WeightConfigVO> vos = new ArrayList<>(configs.size());
        for (KnowledgeWeightConfig config : configs) {
            KnowledgePoint point = pointMap.get(config.getKnowledgePointId());
            vos.add(WeightConfigVO.builder()
                    .id(config.getId())
                    .chapterId(config.getChapterId())
                    .knowledgePointId(config.getKnowledgePointId())
                    .knowledgePointName(point == null ? null : point.getName())
                    .weight(config.getWeight())
                    .baseQuestionCount(config.getBaseQuestionCount())
                    .maxQuestionCount(config.getMaxQuestionCount())
                    .difficultyDistribution(config.getDifficultyDistribution())
                    .isActive(config.getIsActive() != null && config.getIsActive() == 1)
                    .build());
        }
        return vos;
    }

    /**
     * 写操作仅限管理员：JWT 令牌无角色信息，通过 system-api 编程式校验
     */
    private void requireAdmin() {
        Long userId = SecurityUtils.getUserId();
        if (!userService.isAdmin(userId)) {
            log.warn("权限不足: userId={} 尝试执行权重配置写操作", userId);
            throw new BusinessException(ErrorMessageConstant.PERMISSION_DENIED_ADMIN);
        }
    }
}
