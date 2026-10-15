package com.amlve.teach.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * PPT模板实体，对应数据库 teach_ppt_template 表
 * 一套完整模板 = 视觉主题（theme_code 指向 CSS 文件）+ 内容结构（structure_md 为 MD 骨架）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teach_ppt_template")
public class TeachPptTemplate extends BaseEntity {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板名称 */
    @TableField("name")
    private String name;

    /** 模板说明 */
    @TableField("description")
    private String description;

    /** 视觉主题编码（对应 marp-themes 目录下 CSS 文件名，如 java-default） */
    @TableField("theme_code")
    private String themeCode;

    /** 内容结构（MD 骨架，AI 生成 PPT 时填充此结构的占位符） */
    @TableField("structure_md")
    private String structureMd;

    /** 预览图URL */
    @TableField("preview_image")
    private String previewImage;

    /** 是否系统预设：0=否，1=是（系统模板仅可选用，禁止教师修改） */
    @TableField("is_system")
    private Integer isSystem;

    /** 教师ID（自定义模板归属人，系统模板为空） */
    @TableField("teacher_id")
    private Long teacherId;

    /** 是否启用：1=启用，0=停用 */
    @TableField("is_active")
    private Integer isActive;

    /** 排序值（系统模板内排序） */
    @TableField("sort_order")
    private Integer sortOrder;
}
