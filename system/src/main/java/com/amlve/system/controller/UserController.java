package com.amlve.system.controller;

import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.system.dto.request.PasswordChangeRequest;
import com.amlve.system.dto.request.UserAddRequest;
import com.amlve.system.dto.request.UserBaseInfoUpdateRequest;
import com.amlve.system.dto.request.UserQueryRequest;
import com.amlve.system.dto.request.UserUpdateRequest;
import com.amlve.system.dto.response.UserDetailResponse;
import com.amlve.system.dto.response.UserInfoResponse;
import com.amlve.system.dto.response.UserListResponse;
import com.amlve.system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户信息与管理员用户管理。
 * <p>
 * 类级只要求登录，管理端接口逐个方法加 hasRole('ADMIN')；本人类接口一律从
 * SecurityUtils 取当前用户，不接受前端传 userId，避免越权读写他人信息
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;

    /**
     * 当前登录用户完整信息（含角色扩展信息）
     */
    @GetMapping("/info")
    public Result<UserInfoResponse> info() {
        return Result.success(userService.getCurrentUser(SecurityUtils.getUserId()));
    }

    /**
     * 本人更新基础信息，白名单外字段被忽略
     */
    @PutMapping("/info/update")
    public Result<Void> updateBaseInfo(@Valid @RequestBody UserBaseInfoUpdateRequest request) {
        userService.updateBaseInfo(SecurityUtils.getUserId(), request);
        return Result.success(null);
    }

    /**
     * 本人修改密码，校验旧密码，成功后旧 token 全部失效
     */
    @PutMapping("/password/change")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(SecurityUtils.getUserId(), request);
        return Result.success(null);
    }

    /**
     * 管理员重置指定用户密码为系统初始密码。唯一入口
     */
    @PutMapping("/password/reset/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> resetPassword(@PathVariable Long userId) {
        return Result.success(userService.resetPassword(userId));
    }

    /**
     * 历史别名，与 /password/reset/{userId} 同一实现。
     * <p>
     * 两个入口做同一件事意味着改密码策略要改两处、权限配两遍、前端还得猜调哪个，
     * 因此定死下线时间：前端全部切到 /password/reset/{userId} 后即可删除
     *
     * @deprecated 自 2026-11-30 起下线，请改用 {@link #resetPassword(Long)}
     */
    @Deprecated
    @PutMapping("/admin/reset-password/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> resetPasswordAlias(@PathVariable Long userId) {
        return Result.success(userService.resetPassword(userId));
    }

    /**
     * 添加用户（可指定角色）
     */
    @PostMapping("/admin/add")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> add(@Valid @RequestBody UserAddRequest request) {
        return Result.success(userService.addUser(request));
    }

    /**
     * 分页查询用户列表，支持按角色/状态/班级筛选
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<UserListResponse>> list(UserQueryRequest request) {
        return Result.success(userService.listUsers(request));
    }

    /**
     * 查看某个用户详情
     */
    @GetMapping("/admin/detail/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserDetailResponse> detail(@PathVariable Long userId) {
        return Result.success(userService.getUserDetail(userId));
    }

    /**
     * 修改用户信息（管理员可改角色与状态）。被操作用户只在路径里出现一次
     */
    @PutMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long userId, @Valid @RequestBody UserUpdateRequest request) {
        userService.updateUser(userId, request);
        return Result.success(null);
    }

    /**
     * 逻辑删除用户，不允许删除自己
     */
    @DeleteMapping("/admin/delete/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return Result.success(null);
    }

    /**
     * 启用/禁用账号，不允许作用于自己
     */
    @PutMapping("/admin/toggle-status/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> toggleStatus(@PathVariable Long userId) {
        userService.toggleStatus(userId);
        return Result.success(null);
    }
}
