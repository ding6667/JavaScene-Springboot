package com.amlve.common.utils;

public class UserContext {
    // 线程隔离容器，存放当前登录userId
    private static final ThreadLocal<Long> USER_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前登录用户ID
     */
    public static void setUserId(Long userId) {
        USER_HOLDER.set(userId);
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getUserId() {
        return USER_HOLDER.get();
    }

    /**
     * 【必须调用】请求结束清除，防止线程复用串号、内存泄漏
     */
    public static void clear() {
        USER_HOLDER.remove();
    }
}
