package com.amlve.system.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.system.constant.ErrorCodeConstant;
import com.amlve.system.constant.ErrorMessageConstant;
import com.amlve.system.mapper.SysRoleMapper;
import com.amlve.system.service.RoleService;
import com.amlve.system.tableEnt.SysRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper roleMapper;

    @Override
    public SysRole require(Long roleId) {
        if (roleId == null) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, ErrorMessageConstant.ROLE_NOT_SELECTED);
        }
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            // 映射不到一律拒绝。以前用枚举兜底，新增角色就得改代码；现在只要插一行 sys_role
            log.warn("sys_role 中不存在该角色: roleId={}", roleId);
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, ErrorMessageConstant.ROLE_NOT_FOUND);
        }
        return role;
    }

    @Override
    public String nameOf(Long roleId) {
        return require(roleId).getRoleName();
    }

    @Override
    public String descOf(Long roleId) {
        return require(roleId).getRoleDesc();
    }

    @Override
    public Long idOf(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, ErrorMessageConstant.ROLE_NAME_NOT_FOUND);
        }
        SysRole role = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleName, roleName));
        if (role == null) {
            log.warn("按角色名查不到 sys_role 记录: roleName={}", roleName);
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, ErrorMessageConstant.ROLE_NAME_NOT_FOUND);
        }
        return role.getId();
    }

    @Override
    public Map<Long, SysRole> allById() {
        List<SysRole> roles = roleMapper.selectList(null);
        return roles.stream().collect(Collectors.toMap(SysRole::getId, Function.identity(), (a, b) -> a));
    }

    @Override
    public boolean isAdmin(Integer roleId) {
        return matches(roleId, ADMIN);
    }

    @Override
    public boolean isTeacher(Integer roleId) {
        return matches(roleId, TEACHER);
    }

    @Override
    public boolean isStudent(Integer roleId) {
        return matches(roleId, STUDENT);
    }

    /**
     * 角色缺失时返回 false 而不是抛错：这些判定常用于「可选过滤」场景，
     * 脏数据不应让整条链路 500
     */
    private boolean matches(Integer roleId, String expectedRoleName) {
        if (roleId == null) {
            return false;
        }
        SysRole role = roleMapper.selectById(roleId.longValue());
        return role != null && expectedRoleName.equals(role.getRoleName());
    }
}
