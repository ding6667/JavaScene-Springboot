package com.amlve.teach.service.impl;

import com.amlve.common.constant.ErrorMessageConstant;
import cn.hutool.core.bean.BeanUtil;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.teach.dto.request.PptTemplateSaveRequest;
import com.amlve.teach.dto.response.PptTemplateDetailVO;
import com.amlve.teach.dto.response.PptTemplateResponse;
import com.amlve.teach.dto.response.ThemeBriefResponse;
import com.amlve.teach.dto.response.ThemeDetailResponse;
import com.amlve.teach.mapper.TeachPptTemplateMapper;
import com.amlve.teach.mapper.TeachPptThemeMapper;
import com.amlve.teach.service.PptTemplateService;
import com.amlve.teach.tableEnt.TeachPptTemplate;
import com.amlve.teach.tableEnt.TeachPptTheme;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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
    @Resource
    private TeachPptThemeMapper teachPptThemeMapper;

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
            throw new BusinessException(ErrorMessageConstant.TEMPLATE_NOT_FOUND);
        }
        // 自定义模板仅归属人可查；系统模板所有教师可查
        if (template.getIsSystem() == null || template.getIsSystem() != 1) {
            if (!SecurityUtils.getUserId().equals(template.getTeacherId())) {
                throw new BusinessException(ErrorMessageConstant.TEMPLATE_NOT_FOUND);
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
        return vo;
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
            throw new BusinessException(ErrorMessageConstant.TEMPLATE_NOT_FOUND);
        }
        if (template.getIsSystem() != null && template.getIsSystem() == 1) {
            throw new BusinessException(ErrorMessageConstant.TEMPLATE_SYSTEM_READONLY);
        }
        if (!SecurityUtils.getUserId().equals(template.getTeacherId())) {
            throw new BusinessException(ErrorMessageConstant.TEMPLATE_NOT_FOUND);
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
    public List<ThemeBriefResponse> listThemes() {
        List<TeachPptTheme> themes = teachPptThemeMapper.selectList(new LambdaQueryWrapper<TeachPptTheme>()
                .eq(TeachPptTheme::getIsActive, 1)
                .eq(TeachPptTheme::getDeleted, 0)
                .orderByAsc(TeachPptTheme::getSortOrder));

        return themes.stream().map(this::toThemeListItem).collect(Collectors.toList());
    }

    @Override
    public ThemeDetailResponse getThemeDetail(String code) {
        TeachPptTheme theme = teachPptThemeMapper.selectOne(new LambdaQueryWrapper<TeachPptTheme>()
                .eq(TeachPptTheme::getThemeCode, code)
                .eq(TeachPptTheme::getDeleted, 0));
        if (theme == null) {
            throw new BusinessException(ErrorMessageConstant.THEME_NOT_FOUND);
        }
        return BeanUtil.toBean(theme, ThemeDetailResponse.class);
    }

    /** 主题转换：version 字段 Integer → String */
    private ThemeBriefResponse toThemeListItem(TeachPptTheme theme) {
        ThemeBriefResponse response = BeanUtil.toBean(theme, ThemeBriefResponse.class);
        response.setVersion(theme.getVersion() == null ? "1" : String.valueOf(theme.getVersion()));
        return response;
    }
}
