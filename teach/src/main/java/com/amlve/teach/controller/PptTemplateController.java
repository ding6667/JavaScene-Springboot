package com.amlve.teach.controller;

import com.amlve.common.request.Result;
import com.amlve.teach.dto.request.PptTemplateSaveRequest;
import com.amlve.teach.dto.response.PptTemplateDetailVO;
import com.amlve.teach.dto.response.PptTemplateResponse;
import com.amlve.teach.dto.response.ThemeBriefResponse;
import com.amlve.teach.dto.response.ThemeDetailResponse;
import com.amlve.teach.service.PptTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PPT 模板管理接口（教师端）：模板列表/详情/自定义模板维护 + 外观主题列表
 */
@RestController
@RequestMapping("/api/teach/ppt/template")
@RequiredArgsConstructor
public class PptTemplateController {

    private final PptTemplateService pptTemplateService;


    /** 模板列表（系统模板 + 本人自定义模板，按 sort_order 升序） */
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/list")
    public Result<List<PptTemplateResponse>> list() {
        return Result.success(pptTemplateService.listTemplates());
    }

    /** 模板详情（含 structure_md 骨架全文，供前端预览） */
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/detail/{id}")
    public Result<PptTemplateDetailVO> detail(@PathVariable Long id) {
        return Result.success(pptTemplateService.getTemplateDetail(id));
    }

    /** 保存自定义模板（id 为空新增，非空修改本人模板；系统模板一律拒绝） */
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/save")
    public Result<Long> save(@Valid @RequestBody PptTemplateSaveRequest request) {
        return Result.success(pptTemplateService.saveTemplate(request));
    }

    // 主题简要信息
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/themes")
    public Result<List<ThemeBriefResponse>> listThemes() {
        return Result.success(pptTemplateService.listThemes());
    }

    // 单个主题完整信息（含 CSS）
    @GetMapping("/themes/{code}")
    public Result<ThemeDetailResponse> getTheme(@PathVariable String code) {
        return Result.success(pptTemplateService.getThemeDetail(code));
    }
}
