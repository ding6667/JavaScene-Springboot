package com.amlve.teach.dto.response;

import lombok.Data;

/**
 * PPT 模板列表项（轻量，不含 structure_md 全文）
 */
@Data
public class PptTemplateResponse {

    /** 模板ID */
    private Long id;

    /** 模板名称 */
    private String name;

    /** 模板说明 */
    private String description;

    /** 视觉主题编码（java-default/java-dark/java-uncover/java-minimal） */
    private String themeCode;

    /** 预览图URL */
    private String previewImage;

    /** 是否系统预设：0=自定义，1=系统 */
    private Integer isSystem;

    /** 排序值 */
    private Integer sortOrder;
}
