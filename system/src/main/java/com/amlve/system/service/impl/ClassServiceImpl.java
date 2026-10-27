package com.amlve.system.service.impl;

import com.amlve.system.constant.ErrorMessageConstant;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.system.dto.request.ClassSaveRequest;
import com.amlve.system.dto.response.ClassListResponse;
import com.amlve.system.dto.response.StudentListItemResponse;
import com.amlve.system.mapper.ClassMapper;
import com.amlve.system.mapper.SysStudentInfoMapper;
import com.amlve.system.mapper.UserMapper;
import com.amlve.system.service.ClassService;
import com.amlve.system.service.RoleService;
import com.amlve.system.tableEnt.ClassInfo;
import com.amlve.system.tableEnt.SysStudentInfo;
import com.amlve.system.tableEnt.User;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private final ClassMapper classMapper;
    private final SysStudentInfoMapper studentInfoMapper;
    private final UserMapper userMapper;

    @Override
    public List<ClassListResponse> list(Long courseId) {
        List<ClassInfo> classes = applyDataScope(classMapper.selectList(Wrappers.<ClassInfo>lambdaQuery()
                .eq(courseId != null, ClassInfo::getCourseId, courseId)
                .orderByDesc(ClassInfo::getId)));
        if (classes.isEmpty()) {
            return List.of();
        }

        // 任课教师姓名批量取，避免逐班查询
        List<Long> teacherIds = classes.stream().map(ClassInfo::getTeacherId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, User> teacherMap = teacherIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(teacherIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        List<ClassListResponse> result = new ArrayList<>(classes.size());
        for (ClassInfo clazz : classes) {
            ClassListResponse response = toResponse(clazz);
            User teacher = clazz.getTeacherId() == null ? null : teacherMap.get(clazz.getTeacherId());
            if (teacher != null) {
                response.setTeacherName(teacher.getRealName());
            }
            result.add(response);
        }
        return result;
    }

    @Override
    public List<StudentListItemResponse> students(Long classId) {
        requireVisibleClass(classId);

        List<SysStudentInfo> rows = studentInfoMapper.selectList(Wrappers.<SysStudentInfo>lambdaQuery()
                .eq(SysStudentInfo::getClassId, classId).orderByAsc(SysStudentInfo::getStudentNo));
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, User> userMap = userMapper.selectBatchIds(rows.stream()
                        .map(SysStudentInfo::getUserId).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        List<StudentListItemResponse> result = new ArrayList<>(rows.size());
        for (SysStudentInfo row : rows) {
            StudentListItemResponse item = new StudentListItemResponse();
            item.setUserId(row.getUserId());
            item.setStudentNo(row.getStudentNo());
            item.setClassId(row.getClassId());
            item.setClassName(row.getClassName());
            item.setGrade(row.getGrade());
            User user = userMap.get(row.getUserId());
            if (user != null) {
                item.setRealName(user.getRealName());
                item.setEnabled(user.getEnabled());
            }
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ClassSaveRequest request) {
        ClassInfo entity = buildEntity(request);
        // studentCount 由服务端统计，不接受前端传入，避免导入与调班后计数漂移
        entity.setStudentCount(0);
        classMapper.insert(entity);
        log.info("新增班级: id={}, name={}, operator={}",
                entity.getId(), request.getClassName(), SecurityUtils.getCurrentUsername());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ClassSaveRequest request) {
        if (classMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCodeConstant.NOT_FOUND, ErrorMessageConstant.CLASS_NOT_FOUND);
        }
        ClassInfo entity = buildEntity(request);
        entity.setId(id);
        classMapper.updateById(entity);
        log.info("更新班级: id={}, name={}, operator={}",
                id, request.getClassName(), SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (classMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCodeConstant.NOT_FOUND, ErrorMessageConstant.CLASS_NOT_FOUND);
        }
        Long studentCount = studentInfoMapper.selectCount(Wrappers.<SysStudentInfo>lambdaQuery()
                .eq(SysStudentInfo::getClassId, id));
        if (studentCount > 0) {
            throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR,
                    "该班仍有 " + studentCount + " 名学生，请先调班或转出后再删除");
        }
        classMapper.deleteById(id);
        log.info("删除班级: id={}, operator={}", id, SecurityUtils.getCurrentUsername());
    }

    private ClassInfo buildEntity(ClassSaveRequest request) {
        ClassInfo entity = new ClassInfo();
        entity.setClassName(request.getClassName());
        entity.setTeacherId(request.getTeacherId());
        entity.setGrade(request.getGrade());
        entity.setCourseId(request.getCourseId());
        return entity;
    }

    /**
     * 按角色收敛可见班级：教师只留自己任课的，学生只留自己所在的那一个班。
     * 不做这层过滤，班级列表就成了全站学生名单的入口
     */
    private List<ClassInfo> applyDataScope(List<ClassInfo> classes) {
        if (SecurityUtils.hasRole(RoleService.ADMIN) || classes.isEmpty()) {
            return classes;
        }
        Long me = SecurityUtils.getUserId();
        if (SecurityUtils.hasRole(RoleService.TEACHER)) {
            return classes.stream().filter(c -> me.equals(c.getTeacherId())).collect(Collectors.toList());
        }
        SysStudentInfo mine = myStudentInfo(me);
        if (mine == null || mine.getClassId() == null) {
            return List.of();
        }
        return classes.stream().filter(c -> mine.getClassId().equals(c.getId()))
                .collect(Collectors.toList());
    }

    /**
     * 校验班级存在，且调用者对其有数据权限
     */
    private ClassInfo requireVisibleClass(Long classId) {
        ClassInfo clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ErrorCodeConstant.NOT_FOUND, ErrorMessageConstant.CLASS_NOT_FOUND);
        }
        if (SecurityUtils.hasRole(RoleService.ADMIN)) {
            return clazz;
        }
        Long me = SecurityUtils.getUserId();
        if (SecurityUtils.hasRole(RoleService.TEACHER)) {
            if (!me.equals(clazz.getTeacherId())) {
                throw new BusinessException(ErrorCodeConstant.ACCESS_DENIED, ErrorMessageConstant.CLASS_NOT_OWNED);
            }
            return clazz;
        }
        SysStudentInfo mine = myStudentInfo(me);
        if (mine == null || !classId.equals(mine.getClassId())) {
            throw new BusinessException(ErrorCodeConstant.ACCESS_DENIED, ErrorMessageConstant.CLASS_ONLY_OWN);
        }
        return clazz;
    }

    private SysStudentInfo myStudentInfo(Long userId) {
        return studentInfoMapper.selectOne(Wrappers.<SysStudentInfo>lambdaQuery()
                .eq(SysStudentInfo::getUserId, userId));
    }

    private ClassListResponse toResponse(ClassInfo clazz) {
        ClassListResponse response = new ClassListResponse();
        response.setClassId(clazz.getId());
        response.setClassName(clazz.getClassName());
        response.setGrade(clazz.getGrade());
        response.setCourseId(clazz.getCourseId());
        response.setTeacherId(clazz.getTeacherId());
        response.setStudentCount(clazz.getStudentCount());
        return response;
    }
}
