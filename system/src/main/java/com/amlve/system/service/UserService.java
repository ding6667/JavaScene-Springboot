package com.amlve.system.service;

import com.amlve.common.request.PageResult;
import com.amlve.system.dto.request.AdminRegisterRequest;
import com.amlve.system.dto.request.PasswordChangeRequest;
import com.amlve.system.dto.request.UserAddRequest;
import com.amlve.system.dto.request.UserBaseInfoUpdateRequest;
import com.amlve.system.dto.request.UserQueryRequest;
import com.amlve.system.dto.request.UserUpdateRequest;
import com.amlve.system.dto.response.LoginResponse;
import com.amlve.system.dto.response.UserDetailResponse;
import com.amlve.system.dto.response.UserInfoResponse;
import com.amlve.system.dto.response.UserListResponse;

public interface UserService {

    /**
     * 登录，签发 token 并给出后端算好的落地页
     *
     * @param clientIp 本次登录来源IP，写入 sys_user.last_login_ip；取不到可传 null
     */
    LoginResponse checkUser(String username, String password, String clientIp);

    /**
     * 学生自助注册，注册即登录
     */
    LoginResponse register(String username, String password);

    /**
     * 管理员注册（系统后台，需初始化密钥 app.admin.init-key）
     */
    LoginResponse registerAdmin(AdminRegisterRequest request);

    /**
     * 当前用户完整信息，含按角色挂载的扩展信息
     */
    UserInfoResponse getCurrentUser(Long userId);

    /**
     * 管理员视角的用户详情
     */
    UserDetailResponse getUserDetail(Long userId);

    /**
     * 本人更新基础信息，仅白名单字段生效
     */
    void updateBaseInfo(Long userId, UserBaseInfoUpdateRequest request);

    /**
     * 本人修改密码，需校验旧密码；成功后吊销该用户此前签发的全部 token
     */
    void changePassword(Long userId, PasswordChangeRequest request);

    /**
     * 管理员重置密码为系统初始密码，返回该初始密码
     */
    String resetPassword(Long userId);

    /**
     * 新增用户，按角色自动补建扩展记录
     *
     * @return 新用户ID
     */
    Long addUser(UserAddRequest request);

    /**
     * 更新用户。被操作用户由路径给出，不接受 body 里再传一份
     */
    void updateUser(Long userId, UserUpdateRequest request);

    void deleteUser(Long userId);

    /**
     * 启用/禁用切换，不允许作用于操作人自己；禁用成功后吊销该用户全部 token
     */
    void toggleStatus(Long userId);

    PageResult<UserListResponse> listUsers(UserQueryRequest request);

    /**
     * 判断「他人」是否为教师，供跨模块调用；当前登录用户请用 @PreAuthorize
     */
    boolean isTeacher(Long userId);

    boolean isAdmin(Long userId);
}
