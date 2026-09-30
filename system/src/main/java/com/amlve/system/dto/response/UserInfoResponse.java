package com.amlve.system.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应（不含密码等敏感字段）
 */
@Data
public class UserInfoResponse {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 学号（学生角色，教师/管理员为 null）
     */
    private String studentNo;

    /**
     * 班级（学生角色，教师/管理员为 null）
     */
    private String className;

    /**
     * 专业（学生角色，教师/管理员为 null）
     */
    private String major;

    /**
     * 职称（教师角色，学生/管理员为 null）
     */
    private String title;

    /**
     * 角色ID
     */
    private String roleId;

    /**
     * 角色名称（STUDENT / TEACHER）
     */
    private String roleName;

    /**
     * 账号状态：0-禁用，1-启用
     */
    private Integer enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
