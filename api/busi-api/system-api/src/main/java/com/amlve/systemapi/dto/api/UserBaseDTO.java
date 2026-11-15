package com.amlve.systemapi.dto.api;

import lombok.Data;

/**
 * 用户基础信息，跨模块调用用，不含密码等敏感字段
 */
@Data
public class UserBaseDTO {

    private Long userId;

    private String username;

    private String realName;

    /**
     * STUDENT / TEACHER / ADMIN
     */
    private String roleName;

    /**
     * 学生学号 / 教师工号
     */
    private String userNo;

    private Long classId;

    private String className;

    /**
     * 年级（学生档案 sys_student_info.grade），用于匹配「全年级作业」；
     * 非学生或档案未完善时为 null
     */
    private String grade;

    /**
     * 0=禁用，1=启用
     */
    private Integer enabled;
}
