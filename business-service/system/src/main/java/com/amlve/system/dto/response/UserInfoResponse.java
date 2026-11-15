package com.amlve.system.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前用户完整信息：基础信息 + 按角色挂载的扩展信息，避免前端二次请求
 */
@Data
public class UserInfoResponse {

    private Long userId;

    private String username;

    private String realName;

    private String email;

    private String phone;

    private String avatar;

    /**
     * 角色ID：1=学生，2=教师，3=管理员
     */
    private Integer roleId;

    /**
     * 角色名：STUDENT / TEACHER / ADMIN
     */
    private String roleName;

    /**
     * 角色中文描述
     */
    private String roleDesc;

    /**
     * 账号状态：0=禁用，1=启用
     */
    private Integer enabled;

    private LocalDateTime lastLoginTime;

    /**
     * 角色扩展信息：STUDENT 为 StudentInfoResponse，TEACHER 为 TeacherInfoResponse，ADMIN 为 AdminInfoResponse。
     * 缺扩展记录时为对应 Response 类的空对象而非 null，前端不必判空分支
     */
    private Object extInfo;
}
