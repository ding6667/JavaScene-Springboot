package com.amlve.system.controller;

import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.system.dto.request.UserCreateRequest;
import com.amlve.system.dto.request.UserUpdateRequest;
import com.amlve.system.dto.response.UserInfoResponse;
import com.amlve.system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理接口（仅管理员）
 * 说明：JwtAuthenticationFilter 构建的认证令牌不含角色信息，
 * 管理员角色的细粒度校验由 Service 层编程式校验完成（SecurityUtils 取当前用户后查库比对角色）。
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public Result<UserInfoResponse> info() {
        return Result.success(userService.getUserInfo(SecurityUtils.getUserId()));
    }

    /**
     * 新增用户（仅管理员）
     */
    @PostMapping("/create")
    public Result<UserInfoResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.createUser(request));
    }

    /**
     * 更新用户（仅管理员）
     */
    @PutMapping("/update")
    public Result<UserInfoResponse> update(@Valid @RequestBody UserUpdateRequest request) {
        return Result.success(userService.updateUser(request));
    }

    /**
     * 删除用户（仅教师，逻辑删除）
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success(null);
    }

    /**
     * 分页查询用户列表（仅管理员）
     */
    @GetMapping("/records")
    public Result<PageResult<UserInfoResponse>> records(@RequestParam(defaultValue = "1") Long pageNum
            , @RequestParam(defaultValue = "10") Long pageSize
            , @RequestParam(required = false) String keyword) {
        return Result.success(userService.records(pageNum, pageSize, keyword));
    }
}
