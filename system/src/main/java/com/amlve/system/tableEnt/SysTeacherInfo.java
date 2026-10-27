package com.amlve.system.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 教师信息扩展表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_teacher_info")
public class SysTeacherInfo extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /**
     * 工号，唯一
     */
    @TableField("teacher_no")
    private String teacherNo;

    /**
     * 职称：讲师/副教授/教授
     */
    @TableField("title")
    private String title;

    @TableField("college")
    private String college;

    /**
     * 所属系/教研室
     */
    @TableField("department")
    private String department;

    @TableField("office")
    private String office;

    @TableField("research_area")
    private String researchArea;
}
