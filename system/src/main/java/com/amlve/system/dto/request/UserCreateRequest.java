package com.amlve.system.dto.request;

import com.amlve.system.constant.BusinessConstant;
import com.amlve.system.constant.ErrorMessageConstant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增用户请求（教师操作）
 */
@Data
public class UserCreateRequest {

    /**
     * 登录账号（唯一）
     */
    @NotBlank(message = BusinessConstant.REGISTER_USERNAME_EMPTY)
    private String username;

    /**
     * 密码（明文传输，服务端 BCrypt 加密存储）
     */
    @NotBlank(message = BusinessConstant.REGISTER_PASSWORD_EMPTY)
    @Size(min = 6, max = 32, message = ErrorMessageConstant.USER_PASSWORD_SIZE)
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 邮箱
     */
    @Email(message = ErrorMessageConstant.USER_EMAIL_INVALID)
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 角色名称（STUDENT / TEACHER）
     */
    @NotBlank(message = ErrorMessageConstant.USER_ROLE_EMPTY)
    private String roleName;
}
