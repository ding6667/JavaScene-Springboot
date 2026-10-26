package com.amlve.system.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.common.utils.JWTUtil;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.common.security.CustomUserDetails;
import com.amlve.system.constant.ErrorCodeConstant;
import com.amlve.system.constant.ErrorMessageConstant;
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
import com.amlve.system.mapper.SysStudentInfoMapper;
import com.amlve.system.mapper.SysTeacherInfoMapper;
import com.amlve.system.mapper.UserMapper;
import com.amlve.system.service.AdminInfoService;
import com.amlve.system.service.RoleService;
import com.amlve.system.service.StudentInfoService;
import com.amlve.system.service.TeacherInfoService;
import com.amlve.system.service.TokenRevocationService;
import com.amlve.system.service.UserService;
import com.amlve.system.tableEnt.SysRole;
import com.amlve.system.tableEnt.SysStudentInfo;
import com.amlve.system.tableEnt.SysTeacherInfo;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService,
        com.amlve.systemapi.service.UserService {
    private final UserMapper userMapper;
    private final JWTUtil jwtUtil;
    private final RoleService roleService;

    @Value("${app.admin.init-key:}")
    private String adminInitKey;

    /**
     * 批量导入与管理员重置密码时的初始密码
     */
    @Value("${app.user.default-password:123456}")
    private String defaultPassword;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RouteResolver routeResolver;
    private final TokenRevocationService tokenRevocationService;
    private final StudentInfoService studentInfoService;
    private final TeacherInfoService teacherInfoService;
    private final AdminInfoService adminInfoService;
    private final SysStudentInfoMapper studentInfoMapper;
    private final SysTeacherInfoMapper teacherInfoMapper;

    @Override
    public LoginResponse checkUser(String username, String password, String clientIp) {
        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
            Authentication authentication = authenticationManager.authenticate(authToken);
            CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
            recordLogin(user.getUserId(), clientIp);
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
    public LoginResponse register(String username, String password) {
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
        // 自助注册只产生学生，教师/管理员走管理端。角色ID从 sys_role 反查，不再写死 1
        user.setRoleId(roleService.idOf(RoleService.STUDENT).intValue());
        userMapper.insert(user);
        studentInfoService.createEmpty(user.getId());

        // 3. 注册即登录，返回 token + 角色 + 落地页
        log.info("用户注册成功: {}", username);
        return buildLogin(user.getId(), user.getUsername(), List.of(RoleService.STUDENT));
    }

    /**
     * 管理员注册：系统仅支持一名管理员，通过后台初始化密钥（app.admin.init-key）注册，注册即登录
     */
    @Override
    public LoginResponse registerAdmin(AdminRegisterRequest request) {
        if (!StringUtils.hasText(adminInitKey) || !adminInitKey.equals(request.getAdminKey())) {
            log.warn("管理员注册被拒绝: 初始化密钥缺失或错误, 用户名: {}", request.getUsername());
            throw new BusinessException(ErrorCodeConstant.PERMISSION_DENIED, ErrorMessageConstant.ADMIN_INIT_KEY_INVALID);
        }
        Long adminRoleId = roleService.idOf(RoleService.ADMIN);
        Long adminCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRoleId, adminRoleId));
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
        user.setRoleId(adminRoleId.intValue());
        userMapper.insert(user);
        adminInfoService.createEmpty(user.getId());
        log.info("管理员注册成功: {}", request.getUsername());
        return buildLogin(user.getId(), user.getUsername(), List.of(RoleService.ADMIN));
    }

    /**
     * 记录最后登录时间与IP。失败不影响登录本身，只是少一条审计信息
     */
    private void recordLogin(Long userId, String clientIp) {
        try {
            User update = new User();
            update.setId(userId);
            update.setLastLoginTime(LocalDateTime.now());
            update.setLastLoginIp(clientIp);
            userMapper.updateById(update);
        } catch (Exception e) {
            log.warn("记录登录时间失败: userId={}", userId, e);
        }
    }

    /**
     * 签发 token 并组装登录响应，token 的 roles claim 与返回给前端的 roles 保持同源
     */
    private LoginResponse buildLogin(Long userId, String username, List<String> roles) {
        return LoginResponse.builder()
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
    public UserInfoResponse getCurrentUser(Long userId) {
        User user = requireUser(userId);
        UserInfoResponse vo = new UserInfoResponse();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setRoleId(user.getRoleId());
        SysRole role = roleService.require(user.getRoleId().longValue());
        vo.setRoleName(role.getRoleName());
        vo.setRoleDesc(role.getRoleDesc());
        vo.setEnabled(user.getEnabled());
        vo.setLastLoginTime(user.getLastLoginTime());
        // 按角色挂载扩展信息，前端一次请求拿全，不用再来一发接口
        vo.setExtInfo(loadExtInfo(user.getId(), user.getRoleId()));
        return vo;
    }

    @Override
    public UserDetailResponse getUserDetail(Long userId) {
        User user = requireUser(userId);
        UserDetailResponse vo = new UserDetailResponse();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setRoleId(user.getRoleId());
        vo.setRoleName(roleService.nameOf(user.getRoleId().longValue()));
        vo.setEnabled(user.getEnabled());
        vo.setExtInfo(loadExtInfo(user.getId(), user.getRoleId()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBaseInfo(Long userId, UserBaseInfoUpdateRequest request) {
        requireUser(userId);
        // 逐个 set 而非 BeanUtils.copyProperties：白名单外的字段即使前端传了也进不了 UPDATE 语句
        User update = new User();
        update.setId(userId);
        update.setRealName(request.getRealName());
        update.setEmail(request.getEmail());
        update.setPhone(request.getPhone());
        update.setAvatar(request.getAvatar());
        userMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("修改密码失败，旧密码不匹配: userId={}", userId);
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "旧密码不正确");
        }
        writeNewPassword(user.getId(), request.getNewPassword(), "password_changed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetPassword(Long userId) {
        requireUser(userId);
        writeNewPassword(userId, defaultPassword, "password_reset_by_admin");
        log.info("管理员[{}]重置用户密码: userId={}", SecurityUtils.getCurrentUsername(), userId);
        return defaultPassword;
    }

    /**
     * 写入新密码并立即吊销该用户此前签发的全部 token，两步不可拆
     */
    private void writeNewPassword(Long userId, String rawPassword, String reason) {
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.updateById(update);
        tokenRevocationService.revokeAllTokens(userId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addUser(UserAddRequest request) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())) > 0) {
            throw new BusinessException(ErrorCodeConstant.REGISTER_ERROR, ErrorMessageConstant.USER_USERNAME_EXIST);
        }
        SysRole role = roleService.require(request.getRoleId().longValue());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(
                StringUtils.hasText(request.getPassword()) ? request.getPassword() : defaultPassword));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRoleId(role.getId().intValue());
        user.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        userMapper.insert(user);

        // 按角色补一条空扩展记录，保证 /api/user/info 的 extInfo 恒有值
        createEmptyExtInfo(user.getId(), user.getRoleId());
        log.info("管理员[{}]新增用户: id={}, role={}", SecurityUtils.getCurrentUsername(), user.getId(), role.getRoleName());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, UserUpdateRequest request) {
        Long operatorId = SecurityUtils.getUserId();
        User exist = requireUser(userId);
        boolean touchingSelf = operatorId.equals(userId);

        User update = new User();
        update.setId(exist.getId());
        if (request.getRealName() != null) {
            update.setRealName(request.getRealName());
        }
        if (request.getEmail() != null) {
            update.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            update.setPhone(request.getPhone());
        }
        if (request.getAvatar() != null) {
            update.setAvatar(request.getAvatar());
        }
        if (request.getEnabled() != null) {
            if (touchingSelf) {
                throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR, "不能在本人的编辑接口里改账号状态，请用启用/禁用接口");
            }
            update.setEnabled(request.getEnabled());
        }
        boolean roleChanged = request.getRoleId() != null && !request.getRoleId().equals(exist.getRoleId());
        if (roleChanged) {
            if (touchingSelf) {
                // 管理员把自己降成学生 = 当场失去所有管理端权限，且没人能再把他改回来
                throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR, "不能修改当前登录账号的角色");
            }
            SysRole role = roleService.require(request.getRoleId().longValue());
            if (roleService.isAdmin(exist.getRoleId()) && !roleService.isAdmin(role.getId().intValue())) {
                requireAnotherAdmin(exist.getId());
            }
            update.setRoleId(role.getId().intValue());
            // 角色变更要给新角色准备扩展记录，否则新角色下 /api/user/info 查不到东西
            createEmptyExtInfo(exist.getId(), update.getRoleId());
        }
        userMapper.updateById(update);
        log.info("管理员[{}]更新用户 userId={}, 角色变更={}, 状态变更={}, 字段={}",
                SecurityUtils.getCurrentUsername(), userId, roleChanged,
                request.getEnabled() != null, "realName/email/phone/avatar");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId.equals(userId)) {
            throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR, ErrorMessageConstant.USER_DELETE_SELF);
        }
        User exist = requireUser(userId);
        if (roleService.isAdmin(exist.getRoleId())) {
            requireAnotherAdmin(userId);
        }
        // 必须先释放账号名再删：updateById 带 deleted=0 条件，软删之后再改就改不动了
        releaseUsername(userId, exist.getUsername());
        userMapper.deleteById(userId);
        // 与过滤器重复但必要：过滤器靠查库判定，这里显式吊销让 token 立即失效，
        // 也保证日后若改成 token 存角色时不会漏
        tokenRevocationService.revokeAllTokens(userId, "account_deleted");
        log.info("管理员[{}]删除用户 userId={}, 原角色ID={}",
                SecurityUtils.getCurrentUsername(), userId, exist.getRoleId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long userId) {
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId.equals(userId)) {
            // 禁用自己是把自己锁在系统外，且此后无人能再启用
            throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR, "不能禁用或启用当前登录账号");
        }
        User exist = requireUser(userId);
        boolean disabling = Integer.valueOf(1).equals(exist.getEnabled());
        if (disabling && roleService.isAdmin(exist.getRoleId())) {
            requireAnotherAdmin(userId);
        }
        User update = new User();
        update.setId(userId);
        update.setEnabled(disabling ? 0 : 1);
        userMapper.updateById(update);
        if (disabling) {
            tokenRevocationService.revokeAllTokens(userId, "account_disabled");
        }
        log.info("管理员[{}]切换用户状态: userId={}, enabled={}",
                SecurityUtils.getCurrentUsername(), userId, update.getEnabled());
    }

    /**
     * 系统里必须留一个能登录的管理员，否则所有管理端接口永久无人可用
     */
    private void requireAnotherAdmin(Long excludingUserId) {
        Long remaining = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getRoleId, roleService.idOf(RoleService.ADMIN))
                .eq(User::getEnabled, 1)
                .ne(User::getId, excludingUserId));
        if (remaining == null || remaining == 0) {
            throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR,
                    "系统必须保留至少一名启用状态的管理员");
        }
    }

    /**
     * 逻辑删除后释放账号名：sys_user.username 的唯一索引包含软删行，
     * 不释放的话被删用户的账号/学号将永久无法重新注册
     */
    private void releaseUsername(Long userId, String username) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        User update = new User();
        update.setId(userId);
        update.setUsername(username + "#deleted#" + userId);
        userMapper.updateById(update);
    }

    @Override
    public PageResult<UserListResponse> listUsers(UserQueryRequest request) {
        long current = request.getPageNum() == null ? 1L : request.getPageNum();
        long size = request.getPageSize() == null ? 10L : request.getPageSize();
        String keyword = request.getKeyword();

        // 班级筛选落在 sys_student_info，先取该班 userId 再回主表过滤
        List<Long> classUserIds = null;
        if (request.getClassId() != null) {
            classUserIds = studentInfoMapper.selectList(Wrappers.<SysStudentInfo>lambdaQuery()
                            .select(SysStudentInfo::getUserId).eq(SysStudentInfo::getClassId, request.getClassId()))
                    .stream().map(SysStudentInfo::getUserId).collect(Collectors.toList());
            if (classUserIds.isEmpty()) {
                return PageResult.empty(current, size);
            }
        }

        LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery()
                .eq(request.getRoleId() != null, User::getRoleId, request.getRoleId())
                .eq(request.getEnabled() != null, User::getEnabled, request.getEnabled())
                .in(classUserIds != null, User::getId, classUserIds == null ? List.of() : classUserIds)
                .and(StringUtils.hasText(keyword), w -> w.like(User::getUsername, keyword)
                        .or().like(User::getRealName, keyword)
                        .or().like(User::getPhone, keyword))
                .orderByDesc(User::getCreateTime);

        IPage<User> pageData = userMapper.selectPage(new Page<>(current, size), wrapper);
        List<User> rows = pageData.getRecords();
        if (rows.isEmpty()) {
            return PageResult.of(pageData.getTotal(), pageData.getCurrent(), pageData.getSize(), List.of());
        }
        return PageResult.of(pageData.getTotal(), pageData.getCurrent(), pageData.getSize(),
                assembleListRows(rows));
    }

    /**
     * 列表行组装：学号/工号与班级来自扩展表，一次批量取，避免逐行查询
     */
    private List<UserListResponse> assembleListRows(List<User> rows) {
        List<Long> userIds = rows.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, SysStudentInfo> studentMap = studentInfoMapper.selectList(
                        Wrappers.<SysStudentInfo>lambdaQuery().in(SysStudentInfo::getUserId, userIds))
                .stream().collect(Collectors.toMap(SysStudentInfo::getUserId, Function.identity(), (a, b) -> a));
        Map<Long, SysTeacherInfo> teacherMap = teacherInfoMapper.selectList(
                        Wrappers.<SysTeacherInfo>lambdaQuery().in(SysTeacherInfo::getUserId, userIds))
                .stream().collect(Collectors.toMap(SysTeacherInfo::getUserId, Function.identity(), (a, b) -> a));
        // 角色字典只有三行，一次取全再逐行匹配，避免每行两查询
        Map<Long, SysRole> roleMap = roleService.allById();

        List<UserListResponse> result = new ArrayList<>(rows.size());
        for (User user : rows) {
            UserListResponse vo = new UserListResponse();
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setPhone(user.getPhone());
            vo.setEmail(user.getEmail());
            vo.setRoleId(user.getRoleId());
            SysRole role = roleMap.get(user.getRoleId().longValue());
            if (role != null) {
                vo.setRoleName(role.getRoleName());
                vo.setRoleDesc(role.getRoleDesc());
            }
            vo.setEnabled(user.getEnabled());
            vo.setLastLoginTime(user.getLastLoginTime());
            vo.setCreateTime(user.getCreateTime());
            SysStudentInfo student = studentMap.get(user.getId());
            if (student != null) {
                vo.setUserNo(student.getStudentNo());
                vo.setClassName(student.getClassName());
            } else {
                SysTeacherInfo teacher = teacherMap.get(user.getId());
                if (teacher != null) {
                    vo.setUserNo(teacher.getTeacherNo());
                }
            }
            result.add(vo);
        }
        return result;
    }

    private Object loadExtInfo(Long userId, Integer roleId) {
        if (roleService.isStudent(roleId)) {
            return studentInfoService.getByUserId(userId);
        }
        if (roleService.isTeacher(roleId)) {
            return teacherInfoService.getByUserId(userId);
        }
        return adminInfoService.getByUserId(userId);
    }

    private void createEmptyExtInfo(Long userId, Integer roleId) {
        if (roleService.isStudent(roleId)) {
            studentInfoService.createEmpty(userId);
        } else if (roleService.isTeacher(roleId)) {
            teacherInfoService.createEmpty(userId);
        } else {
            adminInfoService.createEmpty(userId);
        }
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeConstant.USER_NOT_FOUND, ErrorMessageConstant.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 供 knowledge 等模块经 system-api 跨模块调用，校验的是「他人」角色，注解替代不了
     */
    @Override
    public boolean isTeacher(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null && roleService.isTeacher(user.getRoleId());
    }

    @Override
    public boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null && roleService.isAdmin(user.getRoleId());
    }
}
