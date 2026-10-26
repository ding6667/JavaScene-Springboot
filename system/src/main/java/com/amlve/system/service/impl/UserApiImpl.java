package com.amlve.system.service.impl;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.system.mapper.SysStudentInfoMapper;
import com.amlve.system.mapper.SysTeacherInfoMapper;
import com.amlve.system.mapper.UserMapper;
import com.amlve.system.service.RoleService;
import com.amlve.system.tableEnt.SysRole;
import com.amlve.system.tableEnt.SysStudentInfo;
import com.amlve.system.tableEnt.SysTeacherInfo;
import com.amlve.system.tableEnt.User;
import com.amlve.systemapi.dto.api.UserBaseDTO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserApiImpl implements com.amlve.systemapi.service.UserApi {

    /**
     * IN 上限，防止调用方一次传进上千个ID把 SQL 撑爆
     */
    private static final int MAX_BATCH_QUERY_SIZE = 500;

    private final UserMapper userMapper;
    private final SysStudentInfoMapper studentInfoMapper;
    private final SysTeacherInfoMapper teacherInfoMapper;
    private final RoleService roleService;

    @Override
    public Result<UserBaseDTO> getUserById(Long userId) {
        if (userId == null) {
            return Result.success(null);
        }
        List<UserBaseDTO> rows = toDTOList(List.of(userId));
        return Result.success(rows.isEmpty() ? null : rows.get(0));
    }

    @Override
    public Result<List<UserBaseDTO>> getUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Result.success(List.of());
        }
        if (userIds.size() > MAX_BATCH_QUERY_SIZE) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID,
                    "单次最多查询 " + MAX_BATCH_QUERY_SIZE + " 个用户");
        }
        return Result.success(toDTOList(userIds));
    }

    @Override
    public Result<Long> getUserIdByStudentNo(String studentNo) {
        SysStudentInfo info = studentInfoMapper.selectOne(Wrappers.<SysStudentInfo>lambdaQuery()
                .eq(SysStudentInfo::getStudentNo, studentNo));
        // 查不到返回 null 而不是报错，让调用方自己决定是提示还是跳过
        return Result.success(info == null ? null : info.getUserId());
    }

    @Override
    public Result<Boolean> isUserInClass(Long userId, Long classId) {
        if (userId == null || classId == null) {
            return Result.success(false);
        }
        Long count = studentInfoMapper.selectCount(Wrappers.<SysStudentInfo>lambdaQuery()
                .eq(SysStudentInfo::getUserId, userId)
                .eq(SysStudentInfo::getClassId, classId));
        return Result.success(count > 0);
    }

    @Override
    public Result<List<Long>> getStudentIdsByClass(Long classId) {
        if (classId == null) {
            return Result.success(List.of());
        }
        return Result.success(studentInfoMapper.selectList(Wrappers.<SysStudentInfo>lambdaQuery()
                        .select(SysStudentInfo::getUserId).eq(SysStudentInfo::getClassId, classId))
                .stream().map(SysStudentInfo::getUserId).collect(Collectors.toList()));
    }

    /**
     * 一次批量取用户 + 学生扩展 + 教师扩展，避免逐行查询
     */
    private List<UserBaseDTO> toDTOList(List<Long> userIds) {
        List<User> users = userMapper.selectBatchIds(userIds);
        if (users.isEmpty()) {
            return List.of();
        }
        Map<Long, SysStudentInfo> studentMap = studentInfoMapper.selectList(
                        Wrappers.<SysStudentInfo>lambdaQuery().in(SysStudentInfo::getUserId, userIds))
                .stream().collect(Collectors.toMap(SysStudentInfo::getUserId, Function.identity(), (a, b) -> a));
        Map<Long, SysTeacherInfo> teacherMap = teacherInfoMapper.selectList(
                        Wrappers.<SysTeacherInfo>lambdaQuery().in(SysTeacherInfo::getUserId, userIds))
                .stream().collect(Collectors.toMap(SysTeacherInfo::getUserId, Function.identity(), (a, b) -> a));
        Map<Long, SysRole> roleMap = roleService.allById();

        List<UserBaseDTO> result = new ArrayList<>(users.size());
        for (User user : users) {
            UserBaseDTO dto = new UserBaseDTO();
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setRealName(user.getRealName());
            // sys_user 已不再冗余 role_name，跨模块也统一从 sys_role 取
            SysRole role = user.getRoleId() == null ? null : roleMap.get(user.getRoleId().longValue());
            if (role != null) {
                dto.setRoleName(role.getRoleName());
            }
            dto.setEnabled(user.getEnabled());
            SysStudentInfo student = studentMap.get(user.getId());
            if (student != null) {
                dto.setUserNo(student.getStudentNo());
                dto.setClassId(student.getClassId());
                dto.setClassName(student.getClassName());
            } else {
                SysTeacherInfo teacher = teacherMap.get(user.getId());
                if (teacher != null) {
                    dto.setUserNo(teacher.getTeacherNo());
                }
            }
            result.add(dto);
        }
        return result;
    }
}
