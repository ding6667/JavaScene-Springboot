package com.amlve.system.dto.request;

import com.amlve.system.constant.ErrorMessageConstant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户请求（教师操作，字段可选更新，密码可选重置）
 */
@Data
public class UserUpdateRequest {

    /**
     * 用户ID
     */
    @NotNull(message = ErrorMessageConstant.USER_ID_NOT_EMPTY)
    private Long id;

    /**
     * 真实姓名（可选）
     */
    private String realName;

    /**
     * 邮箱（可选）
     */
    @Email(message = ErrorMessageConstant.USER_EMAIL_INVALID)
    private String email;

    /**
     * 手机号（可选）
     */
    private String phone;

    /**
     * 学号（可选，学生角色）
     */
    private String studentNo;

    /**
     * 班级（可选，学生角色）
     */
    private String className;

    /**
     * 专业（可选，学生角色）
     */
    private String major;

    /**
     * 职称（可选，教师角色）
     */
    private String title;

    /**
     * 角色名称（可选，STUDENT / TEACHER）
     */
    private String roleName;

    /**
     * 账号状态（可选）：0-禁用，1-启用
     */
    private Integer enabled;

    /**
     * 新密码（可选，传入则重置，服务端 BCrypt 加密）
     */
    @Size(min = 6, max = 32, message = ErrorMessageConstant.USER_PASSWORD_SIZE)
    private String password;
}
