package com.amlve.system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员添加用户请求
 */
@Data
public class UserAddRequest {

    @NotBlank(message = "登录账号不能为空")
    private String username;

    /**
     * 初始密码，为空则使用系统默认初始密码
     */
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    /**
     * 角色ID：1=学生，2=教师，3=管理员
     */
    @NotNull(message = "角色不能为空")
    private Integer roleId;

    /**
     * 账号状态：0=禁用，1=启用，默认启用
     */
    private Integer enabled = 1;
}
