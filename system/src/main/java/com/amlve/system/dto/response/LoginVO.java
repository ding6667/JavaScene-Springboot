package com.amlve.system.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 登录/注册成功响应。注册即登录，与登录同构。
 */
@Data
@Builder
public class LoginVO {

    private String token;

    private Long userId;

    private String username;

    /**
     * 不含 ROLE_ 前缀的角色名列表，供前端路由守卫按 meta.roles 拦截
     */
    private List<String> roles;

    /**
     * 后端算好的落地页，前端直接跳转
     */
    private String defaultRoute;
}
