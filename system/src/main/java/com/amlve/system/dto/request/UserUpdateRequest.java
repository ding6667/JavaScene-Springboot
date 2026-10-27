package com.amlve.system.dto.request;

import com.amlve.system.constant.ErrorMessageConstant;
import jakarta.validation.constraints.Email;
import lombok.Data;

/**
 * 管理员修改用户请求。
 * <p>
 * 不含 userId：被操作用户只出现在路径 PUT /api/user/admin/{userId} 里，
 * body 再带一份会出现「路径 id 与 body id 不一致」这类越权 bug。
 * 也只含基础信息与角色、状态；学号/工号/班级等角色扩展属性走各自的扩展信息接口维护，
 * 密码改走独立的重置接口，避免一个 DTO 同时承担两种权限口径
 */
@Data
public class UserUpdateRequest {

    private String realName;

    @Email(message = ErrorMessageConstant.USER_EMAIL_INVALID)
    private String email;

    private String phone;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 角色ID：1=学生，2=教师，3=管理员，管理员可调整
     */
    private Integer roleId;

    /**
     * 账号状态：0=禁用，1=启用
     */
    private Integer enabled;
}
