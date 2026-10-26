package com.amlve.common.constant;

public class ErrorMessageConstant {
    /**
     * token 过期
     */
    public static final String TOKEN_EXPIRED = "token 过期，请重新登录";
    /**
     * token 篡改、非法
     */
    public static final String TOKEN_INVALID = "token 篡改、非法";

    /**
     * 未授权
     */
    public static final String UNAUTHORIZED_MESSAGE = "用户未登录";
    /**
     *
     */
    public static final String UPDATE_FAILED_INVALID = "更新失败";
    /**
     * 登录失败
     */
    public static final String LOGIN_FAILED = "用户名或密码错误";
    /**
     * 登录成功
     */
    public static final String SUCCESS_MSG = "登录成功";
    /**
     * 账号被管理员禁用（sys_user.enabled = 0）
     */
    public static final String ACCOUNT_DISABLED = "账号已被禁用，请联系管理员";
    /**
     * token 已被登出或踢人操作吊销
     */
    public static final String TOKEN_REVOKED = "登录已失效，请重新登录";
}

