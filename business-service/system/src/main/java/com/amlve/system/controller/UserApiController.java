package com.amlve.system.controller;

import com.amlve.common.request.Result;
import com.amlve.system.service.impl.UserApiImpl;
import com.amlve.systemapi.dto.api.UserBaseDTO;
import com.amlve.systemapi.service.UserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * UserApi 的 HTTP 提供端：Feign 客户端按同一接口调用，方法映射继承自接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/system/user")
public class UserApiController implements UserApi {

    private final UserApiImpl userApi;

    @Override
    public Result<UserBaseDTO> getUserById(Long userId) {
        return userApi.getUserById(userId);
    }

    @Override
    public Result<List<UserBaseDTO>> getUsersByIds(List<Long> userIds) {
        return userApi.getUsersByIds(userIds);
    }

    @Override
    public Result<Long> getUserIdByStudentNo(String studentNo) {
        return userApi.getUserIdByStudentNo(studentNo);
    }

    @Override
    public Result<Boolean> isUserInClass(Long userId, Long classId) {
        return userApi.isUserInClass(userId, classId);
    }

    @Override
    public Result<List<Long>> getStudentIdsByClass(Long classId) {
        return userApi.getStudentIdsByClass(classId);
    }
}
