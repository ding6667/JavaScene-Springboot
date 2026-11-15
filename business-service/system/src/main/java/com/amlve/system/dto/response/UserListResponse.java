package com.amlve.system.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户列表项（管理端）
 */
@Data
public class UserListResponse {

    private Long userId;

    private String username;

    private String realName;

    private String phone;

    private String email;

    private Integer roleId;

    private String roleName;

    private String roleDesc;

    private Integer enabled;

    /**
     * 学生：学号；教师：工号
     */
    private String userNo;

    private String className;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;
}
