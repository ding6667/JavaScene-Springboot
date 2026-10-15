package com.amlve.teach.dto.response;

import lombok.Data;

/**
 * PPT 模板详情（含 structure_md 全文，供前端骨架预览）
 */
@Data
public class PptTemplateDetailVO {

    /** 模板ID */
    private Long id;

    /** 模板名称 */
    private String name;

    /** 模板说明 */
    private String description;

    /** 视觉主题编码 */
    private String themeCode;

    /** 内容结构 MD 骨架全文 */
    private String structureMd;

    /** 预览图URL */
    private String previewImage;

    /** 是否系统预设：0=自定义，1=系统 */
    private Integer isSystem;

    /** 归属教师ID（系统模板为空） */
    private Long teacherId;

    /** 排序值 */
    private Integer sortOrder;
}
