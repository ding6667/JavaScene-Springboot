package com.amlve.system.dto.request;

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

    @Pattern(regexp = "^.{1,50}$", message = "姓名长度需在 1~50 之间")
    private String realName;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 头像URL
     */
    private String avatar;
}
