package com.amlve.knowledge.controller;

import com.amlve.common.request.Result;
import com.amlve.knowledge.dto.request.WeightConfigSaveRequest;
import com.amlve.knowledge.service.KnowledgeWeightConfigService;
import com.amlve.knowledgeapi.dto.response.WeightConfigVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识点权重配置接口
 * <p>
 * 权重配置仅管理员可用：过滤器按用户名加载 UserDetails，其 authorities 已含 ROLE_ADMIN，
 * 故控制层可直接用 hasRole('ADMIN') 拦截；Service 层的 isAdmin 编程式校验保留作双保险。
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge/weight")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class KnowledgeWeightConfigController {

    private final KnowledgeWeightConfigService weightConfigService;

    /**
     * 新增/更新权重配置（管理员用）
     * <p>
     * POST /api/knowledge/weight/save
     * 同一章节+知识点重复提交为更新（upsert）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public Result<Long> saveWeightConfig(@Valid @RequestBody WeightConfigSaveRequest request) {
        log.info("保存权重配置: chapterId={}, knowledgePointId={}, weight={}",
                request.getChapterId(), request.getKnowledgePointId(), request.getWeight());
        Long id = weightConfigService.saveWeightConfig(request);
        return Result.success(id);
    }

    /**
     * 批量保存权重配置（管理员用）
     * <p>
     * POST /api/knowledge/weight/batch
     * 整体一个事务，任一条失败全部回滚
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/batch")
    public Result<Integer> batchSaveWeightConfigs(@Valid @RequestBody List<WeightConfigSaveRequest> requests) {
        log.info("批量保存权重配置: 共 {} 条", requests.size());
        int count = weightConfigService.batchSaveWeightConfigs(requests);
        return Result.success(count);
    }

    /**
     * 按章节查询权重配置列表（管理端展示用）
     * <p>
     * GET /api/knowledge/weight/list?chapterId=5
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public Result<List<WeightConfigVO>> listByChapter(@RequestParam Long chapterId) {
        return Result.success(weightConfigService.listByChapter(chapterId));
    }
}
