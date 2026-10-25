package com.amlve.system.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.common.utils.JWTUtil;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.common.security.CustomUserDetails;
import com.amlve.system.constant.ErrorCodeConstant;
import com.amlve.system.constant.ErrorMessageConstant;
import com.amlve.system.converter.UserConverter;
import com.amlve.system.dto.request.AdminRegisterRequest;
import com.amlve.system.dto.request.UserCreateRequest;
import com.amlve.system.dto.request.UserUpdateRequest;
import com.amlve.system.dto.response.LoginVO;
import com.amlve.system.dto.response.UserInfoResponse;
import com.amlve.system.mapper.UserMapper;
import com.amlve.system.service.TokenRevocationService;
import com.amlve.system.service.UserService;
import com.amlve.system.tableEnt.User;
import com.amlve.system.util.RouteResolver;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService,
        com.amlve.systemapi.service.UserService {
    private final UserMapper userMapper;
    private final JWTUtil jwtUtil;

    @Value("${app.admin.init-key:}")
    private String adminInitKey;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserConverter userConverter;
    private final RouteResolver routeResolver;
    private final TokenRevocationService tokenRevocationService;

    @Override
    public LoginVO checkUser(String username, String password) {
        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
            Authentication authentication = authenticationManager.authenticate(authToken);
            CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
            return buildLogin(user.getUserId(), user.getUsername(), toRoleNames(user.getAuthorities()));
        } catch (DisabledException e) {
            log.warn("禁用账号尝试登录, 用户名: {}", username);
            throw new BusinessException(com.amlve.common.constant.ErrorCodeConstant.UNAUTHORIZED,
                    com.amlve.common.constant.ErrorMessageConstant.ACCOUNT_DISABLED);
        } catch (BadCredentialsException e) {
            log.warn("登录失败, 用户名: {}", username);
            throw new BusinessException(ErrorCodeConstant.LOGIN_ERROR, ErrorMessageConstant.LOGIN_ERROR);
        }
    }

    @Override
    @Transactional
    public LoginVO register(String username, String password) {
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
        user.setRoleId(1);
        user.setRoleName("STUDENT"); // 自助注册仅产生学生，教师/管理员走管理端
        userMapper.insert(user);

        // 3. 注册即登录，返回 token + 角色 + 落地页
        log.info("用户注册成功: {}", username);
        return buildLogin(user.getId(), user.getUsername(), List.of(user.getRoleName()));
    }

    /**
     * 管理员注册：系统仅支持一名管理员，通过后台初始化密钥（app.admin.init-key）注册，注册即登录
     */
    @Override
    public LoginVO registerAdmin(AdminRegisterRequest request) {
        if (!StringUtils.hasText(adminInitKey) || !adminInitKey.equals(request.getAdminKey())) {
            log.warn("管理员注册被拒绝: 初始化密钥缺失或错误, 用户名: {}", request.getUsername());
            throw new BusinessException(ErrorCodeConstant.PERMISSION_DENIED, ErrorMessageConstant.ADMIN_INIT_KEY_INVALID);
        }
        Long adminCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRoleName, "ADMIN"));
        if (adminCount > 0) {
            throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR, ErrorMessageConstant.ADMIN_ALREADY_EXISTS);
        }
        long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(ErrorCodeConstant.REGISTER_ERROR, ErrorMessageConstant.USER_USERNAME_EXIST);
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(1);
        user.setRoleName("ADMIN");
        userMapper.insert(user);
        log.info("管理员注册成功: {}", request.getUsername());
        return buildLogin(user.getId(), user.getUsername(), List.of(user.getRoleName()));
    }

    /**
     * 签发 token 并组装登录响应，token 的 roles claim 与返回给前端的 roles 保持同源
     */
    private LoginVO buildLogin(Long userId, String username, List<String> roles) {
        return LoginVO.builder()
                .token(jwtUtil.generateToken(userId, username, roles))
                .userId(userId)
                .username(username)
                .roles(roles)
                .defaultRoute(routeResolver.resolve(roles))
                .build();
    }

    /**
     * 去掉 Spring Security 的 ROLE_ 前缀，得到业务角色名
     */
    private static List<String> toRoleNames(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .collect(Collectors.toList());
    }

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeConstant.USER_NOT_FOUND, ErrorMessageConstant.USER_NOT_FOUND);
        }
        UserInfoResponse response = userConverter.toResponse(user);
        // 按角色裁剪无关字段：学生不返回教师职称，教师不返回学号/班级/专业（管理员原样返回）
        if ("STUDENT".equals(user.getRoleName())) {
            response.setTitle(null);
        } else if ("TEACHER".equals(user.getRoleName())) {
            response.setStudentNo(null);
            response.setClassName(null);
            response.setMajor(null);
        }
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoResponse createUser(UserCreateRequest request) {
        checkAdminPermission();
        log.info("管理员[{}]新增用户: {}", SecurityUtils.getCurrentUsername(), request.getUsername());
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
        checkAdminPermission();
        log.info("管理员[{}]更新用户: id={}", SecurityUtils.getCurrentUsername(), request.getId());
        User exist = userMapper.selectById(request.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCodeConstant.USER_NOT_FOUND, ErrorMessageConstant.USER_NOT_FOUND);
        }
        boolean passwordChanged = StringUtils.hasText(request.getPassword());
        if (passwordChanged) {
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
        // 角色特定字段（可选更新）：学号/班级/专业属学生，职称属教师
        if (request.getStudentNo() != null) {
            exist.setStudentNo(request.getStudentNo());
        }
        if (request.getClassName() != null) {
            exist.setClassName(request.getClassName());
        }
        if (request.getMajor() != null) {
            exist.setMajor(request.getMajor());
        }
        if (request.getTitle() != null) {
            exist.setTitle(request.getTitle());
        }
        if (request.getRoleName() != null) {
            checkRoleName(request.getRoleName());
            exist.setRoleName(request.getRoleName());
        }
        if (request.getEnabled() != null) {
            exist.setEnabled(request.getEnabled());
        }
        userMapper.updateById(exist);
        // 改密码后旧 token 必须立刻失效，否则「改了密码」不等于「踢掉了拿旧凭证的人」。
        // 角色与 enabled 不需要这一步：过滤器每请求从库重载 authorities 和 enabled，改动即时生效
        if (passwordChanged) {
            tokenRevocationService.revokeAllTokens(exist.getId(), "password_reset");
        }
        return userConverter.toResponse(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        checkAdminPermission();
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId.equals(id)) {
            throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR, ErrorMessageConstant.USER_DELETE_SELF);
        }
        log.info("管理员[{}]删除用户: id={}", SecurityUtils.getCurrentUsername(), id);
        User exist = userMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCodeConstant.USER_NOT_FOUND, ErrorMessageConstant.USER_NOT_FOUND);
        }
        userMapper.deleteById(id);
    }

    @Override
    public PageResult<UserInfoResponse> records(Long pageNum, Long pageSize, String keyword) {
        checkAdminPermission();
        Page<User> page = new Page<>(pageNum, pageSize);
        // 关键词模糊匹配：账号 / 真实姓名 / 手机号 / 学号（对应测试报告 3.7-12）
        LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w.like(User::getUsername, keyword)
                        .or().like(User::getRealName, keyword)
                        .or().like(User::getPhone, keyword)
                        .or().like(User::getStudentNo, keyword))
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
     * 过滤器已把 ROLE_&lt;roleName&gt; 装入 authorities，对当前登录用户的判断可直接用
     * @PreAuthorize("hasRole('TEACHER')")；本方法保留供需要校验「他人」角色的调用点使用
     */
    @Override
    public boolean isTeacher(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null && "TEACHER".equals(user.getRoleName());
    }

    /**
     * 编程式角色校验：判断指定用户是否为管理员（系统仅一名，后台密钥注册产生）
     * 供其他模块（如 knowledge 权重配置）经 system-api 跨模块调用；
     * 对当前登录用户的判断可用 @PreAuthorize("hasRole('ADMIN')") 替代
     */
    @Override
    public boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null && "ADMIN".equals(user.getRoleName());
    }

    /**
     * 编程式角色校验：当前登录用户必须是管理员（唯一，后台密钥注册产生）
     * 等价写法是 @PreAuthorize("hasRole('ADMIN')")，此处保留因需记录被拒 userId 到告警日志
     */
    private void checkAdminPermission() {
        Long userId = SecurityUtils.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null || !"ADMIN".equals(user.getRoleName())) {
            log.warn("权限不足: userId={} 尝试执行管理员专属操作", userId);
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
