package com.amlve.teach.service;

import com.amlve.teach.dto.request.PptTemplateSaveRequest;
import com.amlve.teach.dto.response.MarpThemeVO;
import com.amlve.teach.dto.response.PptTemplateDetailVO;
import com.amlve.teach.dto.response.PptTemplateResponse;

import java.util.List;

/**
 * PPT 模板管理：系统模板 + 教师自定义模板的查询与维护
 */
public interface PptTemplateService {

    /**
     * 模板列表：系统预设模板 + 当前教师自定义模板（按 sort_order 升序）
     */
    List<PptTemplateResponse> listTemplates();

    /**
     * 模板详情（含 structure_md 全文）；自定义模板校验归属，系统模板所有教师可查
     */
    PptTemplateDetailVO getTemplateDetail(Long templateId);

    /**
     * 保存自定义模板：id 为空新增，非空修改本人模板；系统模板一律拒绝
     *
     * @return 模板ID
     */
    Long saveTemplate(PptTemplateSaveRequest request);

    /**
     * 外观主题列表（4 套预置 CSS 主题，常量返回不查库）
     */
    List<MarpThemeVO> listThemes();
}
