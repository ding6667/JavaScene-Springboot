package com.amlve.teach.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.teach.constant.MarpThemeConstants;
import com.amlve.teach.dto.request.PptTemplateSaveRequest;
import com.amlve.teach.dto.response.MarpThemeVO;
import com.amlve.teach.dto.response.PptTemplateDetailVO;
import com.amlve.teach.dto.response.PptTemplateResponse;
import com.amlve.teach.mapper.TeachPptTemplateMapper;
import com.amlve.teach.service.PptTemplateService;
import com.amlve.teach.tableEnt.TeachPptTemplate;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PPT 模板管理业务实现。
 * <p>
 * 模板可见范围：is_system=1 的系统模板对所有教师开放；
 * 自定义模板仅归属人可见、可改，系统模板一律禁止修改。
 */
@Slf4j
@Service
public class PptTemplateServiceImpl implements PptTemplateService {

    @Resource
    private TeachPptTemplateMapper teachPptTemplateMapper;

    @Override
    public List<PptTemplateResponse> listTemplates() {
        Long teacherId = SecurityUtils.getUserId();
        // 条件：is_active=1 AND (is_system=1 OR teacher_id=当前用户)，按 sort_order 升序
        List<TeachPptTemplate> templates = teachPptTemplateMapper.selectList(
                new LambdaQueryWrapper<TeachPptTemplate>()
                        .eq(TeachPptTemplate::getIsActive, 1)
                        .and(w -> w.eq(TeachPptTemplate::getIsSystem, 1)
                                .or().eq(TeachPptTemplate::getTeacherId, teacherId))
                        .orderByAsc(TeachPptTemplate::getSortOrder));
        return templates.stream().map(this::toListItem).collect(Collectors.toList());
    }

    /** 列表项转换：不含 structure_md 全文 */
    private PptTemplateResponse toListItem(TeachPptTemplate template) {
        PptTemplateResponse response = new PptTemplateResponse();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setDescription(template.getDescription());
        response.setThemeCode(template.getThemeCode());
        response.setPreviewImage(template.getPreviewImage());
        response.setIsSystem(template.getIsSystem());
        response.setSortOrder(template.getSortOrder());
        return response;
    }

    @Override
    public PptTemplateDetailVO getTemplateDetail(Long templateId) {
        TeachPptTemplate template = teachPptTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        // 自定义模板仅归属人可查；系统模板所有教师可查
        if (template.getIsSystem() == null || template.getIsSystem() != 1) {
            if (!SecurityUtils.getUserId().equals(template.getTeacherId())) {
                throw new BusinessException("模板不存在或无权访问");
            }
        }

        PptTemplateDetailVO vo = new PptTemplateDetailVO();
        vo.setId(template.getId());
        vo.setName(template.getName());
        vo.setDescription(template.getDescription());
        vo.setThemeCode(template.getThemeCode());
        vo.setStructureMd(template.getStructureMd());
        vo.setPreviewImage(template.getPreviewImage());
        vo.setIsSystem(template.getIsSystem());
        vo.setTeacherId(template.getTeacherId());
        vo.setSortOrder(template.getSortOrder());
        vo.setDesignTokens(resolveDesignTokens(template));
        return vo;
    }

    /** 设计令牌：优先取模板库内 design_tokens，未配置时回退 themeCode 对应的预置令牌 */
    private Map<String, Object> resolveDesignTokens(TeachPptTemplate template) {
        Map<String, Object> stored = template.getDesignTokens();
        if (stored != null && !stored.isEmpty()) {
            return stored;
        }
        return MarpThemeConstants.getDesignTokens(template.getThemeCode());
    }

    @Override
    public Long saveTemplate(PptTemplateSaveRequest request) {
        if (request.getId() == null) {
            return insertTemplate(request);
        }
        return updateTemplate(request);
    }

    /** 新增自定义模板：is_system=0、归属当前教师、默认启用 */
    private Long insertTemplate(PptTemplateSaveRequest request) {
        TeachPptTemplate template = new TeachPptTemplate();
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setThemeCode(request.getThemeCode());
        template.setStructureMd(request.getStructureMd());
        template.setIsSystem(0);
        template.setTeacherId(SecurityUtils.getUserId());
        template.setIsActive(1);
        // 自定义模板排序值为 0（系统模板 1 起），列表中排在系统模板之前
        template.setSortOrder(0);
        teachPptTemplateMapper.insert(template);
        log.info("新增自定义PPT模板: templateId={}, teacherId={}, name={}",
                template.getId(), template.getTeacherId(), template.getName());
        return template.getId();
    }

    /** 修改自定义模板：系统模板拒绝、他人模板拒绝 */
    private Long updateTemplate(PptTemplateSaveRequest request) {
        TeachPptTemplate template = teachPptTemplateMapper.selectById(request.getId());
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        if (template.getIsSystem() != null && template.getIsSystem() == 1) {
            throw new BusinessException("系统模板不可修改");
        }
        if (!SecurityUtils.getUserId().equals(template.getTeacherId())) {
            throw new BusinessException("模板不存在或无权访问");
        }

        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setThemeCode(request.getThemeCode());
        template.setStructureMd(request.getStructureMd());
        teachPptTemplateMapper.updateById(template);
        log.info("修改自定义PPT模板: templateId={}, teacherId={}", template.getId(), template.getTeacherId());
        return template.getId();
    }

    @Override
    public List<MarpThemeVO> listThemes() {
        return MarpThemeConstants.THEMES;
    }
}
