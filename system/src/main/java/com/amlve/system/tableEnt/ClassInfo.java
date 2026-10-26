package com.amlve.system.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 班级主数据。学生归属以 sys_student_info.class_id 为准，本表不存学生列表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_class")
public class ClassInfo extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("class_name")
    private String className;

    /**
     * 任课教师，对应 sys_user.id
     */
    @TableField("teacher_id")
    private Long teacherId;

    /**
     * 年级文本，如 "2024级"
     */
    @TableField("grade")
    private String grade;

    /**
     * 学生人数冗余计数，导入与调班后重算
     */
    @TableField("student_count")
    private Integer studentCount;

    @TableField("course_id")
    private Long courseId;
}
