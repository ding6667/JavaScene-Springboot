package com.amlve.teach.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 备课记录表实体，对应数据库 teach_prepare_record 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teach_prepare_record")
public class TeachPrepareRecord extends BaseEntity {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联教材章节ID */
    @TableField("chapter_id")
    private Long chapterId;

    /** 备课教师ID */
    @TableField("teacher_id")
    private Long teacherId;

    /** 教案内容（Markdown） */
    @TableField("lesson_plan")
    private String lessonPlan;

    /** 重难点清单（Markdown） */
    @TableField("key_points")
    private String keyPoints;

    /** 课后习题（Markdown） */
    private String homework;

    /** 上机实训方案（Markdown） */
    private String lab;

    /** PPT原始Markdown内容 */
    @TableField("pptx_md")
    private String pptxMd;

    /** PPT文件路径 */
    @TableField("pptx_url")
    private String pptxUrl;

    /** 状态（1=成功，0=失败） */
    private Integer status;

    /** 错误信息 */
    @TableField("error_msg")
    private String errorMsg;
}
