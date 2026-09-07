package com.amlve.system.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.common.utils.JWTUtil;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.common.security.CustomUserDetails;
import com.amlve.system.constant.ErrorCodeConstant;
import com.amlve.system.constant.ErrorMessageConstant;
import com.amlve.system.converter.UserConverter;
import com.amlve.system.dto.request.UserCreateRequest;
import com.amlve.system.dto.request.UserUpdateRequest;
import com.amlve.system.dto.response.UserInfoResponse;
import com.amlve.system.mapper.UserMapper;
import com.amlve.system.service.UserService;
import com.amlve.system.tableEnt.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService,
        com.amlve.systemapi.service.UserService {
    private final UserMapper userMapper;
    private final JWTUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserConverter userConverter;

    @Override
    public String checkUser(String username, String password) {
        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
            Authentication authentication = authenticationManager.authenticate(authToken);
            CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
            return jwtUtil.generateToken(user.getUserId(), user.getUsername());
        } catch (BadCredentialsException e) {
            log.warn("登录失败, 用户名: {}", username);
            throw new BusinessException(ErrorCodeConstant.LOGIN_ERROR, ErrorMessageConstant.LOGIN_ERROR);
        }
    }

    @Override
    @Transactional
    public String register(String username, String password) {
        // 1. 检查用户名是否已存在
        long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count > 0) {
            throw new BusinessException(ErrorCodeConstant.REGISTER_ERROR, "用户名已存在");
        }

        // 2. 创建用户，密码使用 BCrypt 加密
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // ✅ 加密存储
        user.setEnabled(1);
        userMapper.insert(user);

        // 3. 返回 JWT（注册即登录，直接返回 Token）
        log.info("用户注册成功: {}", username);
        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeConstant.USER_NOT_FOUND, ErrorMessageConstant.USER_NOT_FOUND);
        }
        return userConverter.toResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoResponse createUser(UserCreateRequest request) {
        checkTeacherPermission();
        log.info("教师[{}]新增用户: {}", SecurityUtils.getCurrentUsername(), request.getUsername());
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(ErrorCodeConstant.REGISTER_ERROR, ErrorMessageConstant.USER_USERNAME_EXIST);
        }
        User user = userConverter.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(1);
        userMapper.insert(user);
        return userConverter.toResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoResponse updateUser(UserUpdateRequest request) {
        checkTeacherPermission();
        log.info("教师[{}]更新用户: id={}", SecurityUtils.getCurrentUsername(), request.getId());
        User exist = userMapper.selectById(request.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCodeConstant.USER_NOT_FOUND, ErrorMessageConstant.USER_NOT_FOUND);
        }
        if (StringUtils.hasText(request.getPassword())) {
            exist.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRealName() != null) {
            exist.setRealName(request.getRealName());
        }
        if (request.getEmail() != null) {
            exist.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            exist.setPhone(request.getPhone());
        }
        if (request.getRoleName() != null) {
            checkRoleName(request.getRoleName());
            exist.setRoleName(request.getRoleName());
        }
        if (request.getEnabled() != null) {
            exist.setEnabled(request.getEnabled());
        }
        userMapper.updateById(exist);
        return userConverter.toResponse(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        checkTeacherPermission();
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId.equals(id)) {
            throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR, ErrorMessageConstant.USER_DELETE_SELF);
        }
        log.info("教师[{}]删除用户: id={}", SecurityUtils.getCurrentUsername(), id);
        User exist = userMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCodeConstant.USER_NOT_FOUND, ErrorMessageConstant.USER_NOT_FOUND);
        }
        userMapper.deleteById(id);
    }

    @Override
    public PageResult<UserInfoResponse> records(Long pageNum, Long pageSize, String keyword) {
        checkTeacherPermission();
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w.like(User::getUsername, keyword)
                        .or().like(User::getRealName, keyword))
                .orderByDesc(User::getCreateTime);
        IPage<User> pageData = userMapper.selectPage(page, wrapper);
        List<UserInfoResponse> voList = pageData.getRecords().stream()
                .map(userConverter::toResponse)
                .collect(Collectors.toList());
        PageResult<UserInfoResponse> result = new PageResult<>();
        result.setRecords(voList);
        result.setTotal(pageData.getTotal());
        result.setCurrent(pageData.getCurrent());
        result.setSize(pageData.getSize());
        return result;
    }

    /**
     * 编程式角色校验：当前登录用户必须是教师
     * 说明：JWT 过滤器构建的认证令牌不含角色信息，无法直接使用 @PreAuthorize 的 hasRole，
     * 此处通过 SecurityUtils 获取当前用户后查库比对角色
     */
    @Override
    public boolean isTeacher(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null && "TEACHER".equals(user.getRoleName());
    }

    private void checkTeacherPermission() {
        Long userId = SecurityUtils.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null || !"TEACHER".equals(user.getRoleName())) {
            log.warn("权限不足: userId={} 尝试执行教师专属操作", userId);
            throw new BusinessException(ErrorCodeConstant.PERMISSION_DENIED, ErrorMessageConstant.USER_PERMISSION_DENIED);
        }
    }

    /**
     * 角色名合法性校验（仅支持 STUDENT / TEACHER）
     */
    private void checkRoleName(String roleName) {
        if (!"STUDENT".equals(roleName) && !"TEACHER".equals(roleName)) {
            throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR, ErrorMessageConstant.USER_ROLE_INVALID);
        }
    }
}
