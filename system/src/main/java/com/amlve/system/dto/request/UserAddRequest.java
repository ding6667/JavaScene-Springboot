package com.amlve.system.dto.request;

import com.amlve.system.constant.ErrorMessageConstant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员添加用户请求
 */
@Data
public class UserAddRequest {

    @NotBlank(message = ErrorMessageConstant.USERNAME_EMPTY)
    private String username;

    /**
     * 初始密码，为空则使用系统默认初始密码
     */
    private String password;

    @NotBlank(message = ErrorMessageConstant.REAL_NAME_EMPTY)
    private String realName;

    @Email(message = ErrorMessageConstant.USER_EMAIL_INVALID)
    private String email;

    private String phone;

    /**
     * 角色ID：1=学生，2=教师，3=管理员
     */
    @NotNull(message = ErrorMessageConstant.ROLE_NOT_SELECTED)
    private Integer roleId;

    /**
     * 账号状态：0=禁用，1=启用，默认启用
     */
    private Integer enabled = 1;
}
