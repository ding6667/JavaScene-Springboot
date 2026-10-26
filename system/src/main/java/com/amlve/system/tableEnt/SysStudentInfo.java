package com.amlve.system.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学生信息扩展表，一行对应一名学生的学籍侧属性
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_student_info")
public class SysStudentInfo extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /**
     * 学号，唯一
     */
    @TableField("student_no")
    private String studentNo;

    @TableField("class_id")
    private Long classId;

    /**
     * 班级名称冗余，导入与调班时由服务端回填，不接受前端传入
     */
    @TableField("class_name")
    private String className;

    @TableField("grade")
    private String grade;

    @TableField("major")
    private String major;

    @TableField("college")
    private String college;

    @TableField("enroll_year")
    private Integer enrollYear;

    /**
     * 学生自填范围不含此列，由管理员维护或按入学年份推算
     */
    @TableField("graduation_year")
    private Integer graduationYear;
}
