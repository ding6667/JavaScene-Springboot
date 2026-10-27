package com.amlve.system.dto.response;

import lombok.Data;

/**
 * 用户详情（管理员视角），extInfo 与 /api/user/info 同构
 */
@Data
public class UserDetailResponse {

    private Long userId;

    private String username;

    private String realName;

    private String email;

    private String phone;

    private String avatar;

    private Integer roleId;

    private String roleName;

    private Integer enabled;

    /**
     * 角色扩展信息：StudentInfoResponse / TeacherInfoResponse / AdminInfoResponse
     */
    private Object extInfo;
}
