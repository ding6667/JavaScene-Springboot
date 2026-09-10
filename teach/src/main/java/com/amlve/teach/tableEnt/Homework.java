package com.amlve.teach.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 作业表实体，对应数据库 teach_homework 表（结构以 master sql.sql 为准）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teach_homework")
public class Homework extends BaseEntity {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作业标题 */
    private String title;

    /** 作业内容/要求 */
    private String content;

    /** 关联章节ID */
    @TableField("chapter_id")
    private Long chapterId;

    /** 截止时间 */
    private LocalDateTime deadline;

    /** 发布教师ID */
    @TableField("teacher_id")
    private Long teacherId;

    /** 发布班级ID（NULL表示全年级） */
    @TableField("class_id")
    private Long classId;

    /** 状态（1=未发布，2=已发布，3=已截止，4=已归档） */
    private Integer status;

    /** 作业总分 */
    @TableField("total_score")
    private Integer totalScore;
}
