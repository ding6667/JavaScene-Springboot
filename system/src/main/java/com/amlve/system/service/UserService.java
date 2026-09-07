package com.amlve.system.service;

import com.amlve.common.request.PageResult;
import com.amlve.system.dto.request.UserCreateRequest;
import com.amlve.system.dto.request.UserUpdateRequest;
import com.amlve.system.dto.response.UserInfoResponse;

public interface UserService {
    String checkUser(String username, String password);

    String register(String username, String password);

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息（不含密码）
     */
    UserInfoResponse getUserInfo(Long userId);

    /**
     * 新增用户（仅教师）
     */
    UserInfoResponse createUser(UserCreateRequest request);

    /**
     * 更新用户（仅教师）
     */
    UserInfoResponse updateUser(UserUpdateRequest request);

    /**
     * 删除用户（仅教师，逻辑删除）
     */
    void deleteUser(Long id);

    /**
     * 分页查询用户列表（仅教师）
     * @param keyword 用户名/真实姓名模糊搜索，可为空
     */
    PageResult<UserInfoResponse> records(Long pageNum, Long pageSize, String keyword);
}
