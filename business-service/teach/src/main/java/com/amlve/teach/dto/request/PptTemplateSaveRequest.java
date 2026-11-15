package com.amlve.teach.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * PPT 模板保存请求（新增/修改自定义模板）
 * id 为空 → 新增；id 非空 → 修改本人自定义模板（系统模板一律拒绝）
 */
@Data
public class PptTemplateSaveRequest {

    /** 模板ID（为空表示新增） */
    private Long id;

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称不能超过100字")
    private String name;

    /** 模板说明 */
    @Size(max = 500, message = "模板说明不能超过500字")
    private String description;

    /** 视觉主题编码（限定 4 套预置主题） */
    @NotBlank(message = "视觉主题不能为空")
    @Pattern(regexp = "java-default|java-dark|java-uncover|java-minimal",
            message = "视觉主题必须是预置的 4 套主题之一")
    private String themeCode;

    /** 内容结构 MD 骨架 */
    @NotBlank(message = "模板结构不能为空")
    private String structureMd;
}
