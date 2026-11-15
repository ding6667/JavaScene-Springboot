package com.amlve.system.dto.request;

import com.amlve.system.constant.ErrorMessageConstant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 本人更新基础信息请求。
 * <p>
 * 白名单仅 realName / email / phone / avatar：username、roleId、roleName、学号、工号
 * 一律不接受，即使前端传入也会被忽略，避免自助接口越权改角色
 */
@Data
public class UserBaseInfoUpdateRequest {

    @Pattern(regexp = "^.{1,50}$", message = ErrorMessageConstant.REAL_NAME_SIZE)
    private String realName;

    @Email(message = ErrorMessageConstant.USER_EMAIL_INVALID)
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = ErrorMessageConstant.PHONE_INVALID)
    private String phone;

    /**
     * 头像URL
     */
    private String avatar;
}
