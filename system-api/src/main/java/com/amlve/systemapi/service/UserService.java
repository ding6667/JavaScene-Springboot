package com.amlve.systemapi.service;

/**
 * 用户对外服务接口（供其他模块调用）
 * <p>
 * 跨模块只允许依赖 system-api 的接口，禁止注入 system 实现类。
 */
public interface UserService {

    /**
     * 判断用户是否为教师角色
     *
     * @param userId 用户ID
     * @return true=教师
     */
    boolean isTeacher(Long userId);

    /**
     * 判断用户是否为管理员角色
     *
     * @param userId 用户ID
     * @return true=管理员
     */
    boolean isAdmin(Long userId);
}
