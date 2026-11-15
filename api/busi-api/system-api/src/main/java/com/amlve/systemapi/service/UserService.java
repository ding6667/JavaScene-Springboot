package com.amlve.systemapi.service;

import com.amlve.common.web.feign.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户对外服务接口（供其他模块调用）
 * <p>
 * 跨模块只允许依赖 system-api 的接口，禁止注入 system 实现类。
 */
@FeignClient(name = "system-service", contextId = "userService", path = "/internal/system/user-service", configuration = FeignConfig.class)
public interface UserService {

    /**
     * 判断用户是否为教师角色
     *
     * @param userId 用户ID
     * @return true=教师
     */
    @PostMapping("/isTeacher")
    boolean isTeacher(@RequestParam("userId") Long userId);

    /**
     * 判断用户是否为管理员角色（系统仅一名，后台密钥注册产生）
     *
     * @param userId 用户ID
     * @return true=管理员
     */
    @PostMapping("/isAdmin")
    boolean isAdmin(@RequestParam("userId") Long userId);
}
