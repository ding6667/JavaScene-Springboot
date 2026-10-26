package com.amlve.system.service.impl;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.system.dto.request.StudentInfoUpdateRequest;
import com.amlve.system.dto.request.StudentQueryRequest;
import com.amlve.system.dto.response.BatchImportResponse;
import com.amlve.system.dto.response.StudentInfoResponse;
import com.amlve.system.dto.response.StudentListItemResponse;
import com.amlve.system.mapper.ClassMapper;
import com.amlve.system.mapper.SysStudentInfoMapper;
import com.amlve.system.mapper.UserMapper;
import com.amlve.system.service.RoleService;
import com.amlve.system.service.StudentInfoService;
import com.amlve.system.tableEnt.ClassInfo;
import com.amlve.system.tableEnt.SysStudentInfo;
import com.amlve.system.tableEnt.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentInfoServiceImpl implements StudentInfoService {

    /**
     * 单次导入上限，与接口文档口径一致
     */
    private static final int MAX_BATCH_SIZE = 200;

    /**
     * 导出上限，防止管理员一次拉穿内存
     */
    private static final int EXPORT_MAX_ROWS = 10000;

    private final SysStudentInfoMapper studentInfoMapper;
    private final UserMapper userMapper;
    private final ClassMapper classMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    @Value("${app.user.default-password:123456}")
    private String defaultPassword;

    @Override
    public StudentInfoResponse getByUserId(Long userId) {
        SysStudentInfo info = selectByUserId(userId);
        StudentInfoResponse vo = new StudentInfoResponse();
        vo.setUserId(userId);
        if (info != null) {
            vo.setStudentNo(info.getStudentNo());
            vo.setClassId(info.getClassId());
            vo.setClassName(info.getClassName());
            vo.setGrade(info.getGrade());
            vo.setMajor(info.getMajor());
            vo.setCollege(info.getCollege());
            vo.setEnrollYear(info.getEnrollYear());
            vo.setGraduationYear(info.getGraduationYear());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSelf(Long userId, StudentInfoUpdateRequest request) {
        SysStudentInfo exist = selectByUserId(userId);
        boolean firstFill = exist == null || !StringUtils.hasText(exist.getStudentNo());

        if (firstFill) {
            if (!StringUtils.hasText(request.getStudentNo())) {
                throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "首次完善必须填写学号");
            }
            if (existsStudentNo(request.getStudentNo())) {
                throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "学号已被占用");
            }
            SysStudentInfo entity = exist == null ? new SysStudentInfo() : exist;
            entity.setUserId(userId);
            entity.setStudentNo(request.getStudentNo());
            // 班级由管理员预先分配，学生侧只读：仅回填已分配班级的冗余名称与年级
            ClassInfo clazz = entity.getClassId() == null ? null : classMapper.selectById(entity.getClassId());
            if (clazz != null) {
                entity.setClassName(clazz.getClassName());
                entity.setGrade(clazz.getGrade());
            }
            entity.setMajor(request.getMajor());
            entity.setCollege(request.getCollege());
            entity.setEnrollYear(request.getEnrollYear());
            saveOrUpdate(entity);
            return;
        }

        // 已完善过：studentNo 与 classId 忽略，只更新可改字段
        SysStudentInfo update = new SysStudentInfo();
        update.setId(exist.getId());
        update.setMajor(request.getMajor());
        update.setCollege(request.getCollege());
        update.setEnrollYear(request.getEnrollYear());
        studentInfoMapper.updateById(update);
    }

    @Override
    public void createEmpty(Long userId) {
        if (studentInfoMapper.selectCount(Wrappers.<SysStudentInfo>lambdaQuery()
                .eq(SysStudentInfo::getUserId, userId)) > 0) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "该用户已存在学生扩展记录");
        }
        SysStudentInfo info = new SysStudentInfo();
        info.setUserId(userId);
        studentInfoMapper.insert(info);
    }

    @Override
    public PageResult<StudentListItemResponse> pageStudents(List<Long> classIds, StudentQueryRequest request) {
        long current = request.getPageNum() == null ? 1L : request.getPageNum();
        long size = request.getPageSize() == null ? 10L : request.getPageSize();

        LambdaQueryWrapper<SysStudentInfo> wrapper = Wrappers.lambdaQuery();
        if (classIds != null) {
            if (classIds.isEmpty()) {
                return PageResult.empty(current, size);
            }
            wrapper.in(SysStudentInfo::getClassId, classIds);
        }
        String keyword = request.getKeyword();
        if (StringUtils.hasText(keyword)) {
            // 姓名存在 sys_user，先按姓名取 userId 再与学号模糊做并集
            List<Long> nameMatched = userMapper.selectList(Wrappers.<User>lambdaQuery()
                            .select(User::getId).like(User::getRealName, keyword))
                    .stream().map(User::getId).collect(Collectors.toList());
            wrapper.and(q -> {
                q.like(SysStudentInfo::getStudentNo, keyword);
                if (!nameMatched.isEmpty()) {
                    q.or().in(SysStudentInfo::getUserId, nameMatched);
                }
            });
        }
        wrapper.orderByAsc(SysStudentInfo::getStudentNo);

        Page<SysStudentInfo> page = studentInfoMapper.selectPage(new Page<>(current, size), wrapper);
        List<SysStudentInfo> rows = page.getRecords();
        if (rows.isEmpty()) {
            return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), List.of());
        }

        Map<Long, User> userMap = loadUsers(rows.stream()
                .map(SysStudentInfo::getUserId).collect(Collectors.toList()));

        List<StudentListItemResponse> records = new ArrayList<>(rows.size());
        for (SysStudentInfo row : rows) {
            StudentListItemResponse vo = new StudentListItemResponse();
            vo.setUserId(row.getUserId());
            vo.setStudentNo(row.getStudentNo());
            vo.setClassId(row.getClassId());
            vo.setClassName(row.getClassName());
            vo.setGrade(row.getGrade());
            User user = userMap.get(row.getUserId());
            if (user != null) {
                vo.setRealName(user.getRealName());
                vo.setEnabled(user.getEnabled());
            }
            records.add(vo);
        }
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public BatchImportResponse batchAdd(Long classId, org.springframework.web.multipart.MultipartFile file) {
        ClassInfo clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ErrorCodeConstant.NOT_FOUND, "班级不存在");
        }

        BatchImportResponse result = new BatchImportResponse();
        Set<String> seenInBatch = new HashSet<>();
        List<String> lines = readLines(file);
        // 学生角色ID整批改一次：sys_role 是权威来源，写死 1 会让换库就崩
        Integer studentRoleId = roleService.idOf(RoleService.STUDENT).intValue();

        for (int i = 0; i < lines.size(); i++) {
            int lineNo = i + 1;
            String line = lines.get(i);
            result.setTotal(result.getTotal() + 1);
            if (result.getTotal() > MAX_BATCH_SIZE) {
                result.addFail(lineNo, "-", "-", "单次导入不能超过 " + MAX_BATCH_SIZE + " 条，后续行已忽略");
                break;
            }
            try {
                importOne(line, clazz, seenInBatch, result, studentRoleId);
            } catch (BusinessException e) {
                // 单条失败不回滚整批：本方法刻意不加事务，每条插入各自提交
                result.addFail(lineNo, extractStudentNo(line), "-", e.getMessage());
            } catch (Exception e) {
                log.warn("批量导入学生异常: lineNo={}, line={}", lineNo, line, e);
                result.addFail(lineNo, extractStudentNo(line), "-", "入库异常: " + e.getMessage());
            }
        }

        refreshStudentCount(classId);
        log.info("批量导入学生完成: classId={}, total={}, success={}, fail={}",
                classId, result.getTotal(), result.getSuccessNum(), result.getFailNum());
        return result;
    }

    @Override
    public List<Long> listUserIdsByClass(Long classId) {
        return studentInfoMapper.selectList(Wrappers.<SysStudentInfo>lambdaQuery()
                        .select(SysStudentInfo::getUserId).eq(SysStudentInfo::getClassId, classId))
                .stream().map(SysStudentInfo::getUserId).collect(Collectors.toList());
    }

    @Override
    public List<Long> listClassIdsByTeacher(Long teacherId) {
        return classMapper.selectList(Wrappers.<ClassInfo>lambdaQuery()
                        .select(ClassInfo::getId).eq(ClassInfo::getTeacherId, teacherId))
                .stream().map(ClassInfo::getId).collect(Collectors.toList());
    }

    @Override
    public boolean existsStudentNo(String studentNo) {
        return studentInfoMapper.selectCount(Wrappers.<SysStudentInfo>lambdaQuery()
                .eq(SysStudentInfo::getStudentNo, studentNo)) > 0;
    }

    @Override
    public String importTemplate() {
        return "# 学生批量导入模板\n"
                + "# 一行一名学生，字段用英文逗号或中文逗号分隔：学号,姓名[,初始密码]\n"
                + "# 以 # 开头的行和空行会被忽略；省略初始密码时使用系统默认密码\n"
                + "# 学号同时作为登录账号，且必须全局唯一\n"
                + "2024001,张三\n"
                + "2024002,李四,Abc123456\n";
    }

    @Override
    public String exportCsv(List<Long> classIds, String keyword) {
        StudentQueryRequest request = new StudentQueryRequest();
        request.setKeyword(keyword);
        // 导出走与列表完全相同的可见范围逻辑，避免两套口径漏一条
        request.setPageSize(EXPORT_MAX_ROWS);
        request.setPageNum(1);
        List<StudentListItemResponse> rows = pageStudents(classIds, request).getRecords();

        StringBuilder csv = new StringBuilder();
        // BOM：Excel 双击打开 UTF-8 CSV 时不乱码。用转义而不是字面字符，避免源码里出现不可见字节
        csv.append('\uFEFF');
        csv.append("学号,姓名,班级,年级,账号状态\n");
        for (StudentListItemResponse row : rows) {
            csv.append(csvCell(row.getStudentNo())).append(',')
                    .append(csvCell(row.getRealName())).append(',')
                    .append(csvCell(row.getClassName())).append(',')
                    .append(csvCell(row.getGrade())).append(',')
                    .append(Integer.valueOf(1).equals(row.getEnabled()) ? "启用" : "禁用")
                    .append('\n');
        }
        return csv.toString();
    }

    /**
     * CSV 单元格转义：包双引号并把内部引号翻倍，防止姓名里带逗号串列
     */
    private String csvCell(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /**
     * 导入一行：学号,姓名[,初始密码]。学号同时作为登录账号
     */
    private void importOne(String line, ClassInfo clazz, Set<String> seenInBatch,
                           BatchImportResponse result, Integer studentRoleId) {
        String[] parts = line.split("[,，\\t]");
        if (parts.length < 2) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "格式应为 学号,姓名[,初始密码]");
        }
        String studentNo = parts[0].trim();
        String realName = parts[1].trim();
        if (!StringUtils.hasText(studentNo) || !StringUtils.hasText(realName)) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "学号与姓名均不能为空");
        }
        // 批内查重不能依赖唯一键异常兜底：唯一键冲突会污染连接且报错不可读
        if (!seenInBatch.add(studentNo)) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "批内学号重复");
        }
        if (existsStudentNo(studentNo)) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "学号已存在");
        }
        if (userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, studentNo)) > 0) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "登录账号(学号)已存在");
        }

        String password = parts.length > 2 && StringUtils.hasText(parts[2].trim())
                ? parts[2].trim() : defaultPassword;

        User user = new User();
        user.setUsername(studentNo);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setEnabled(1);
        user.setRoleId(studentRoleId);
        userMapper.insert(user);

        SysStudentInfo info = new SysStudentInfo();
        info.setUserId(user.getId());
        info.setStudentNo(studentNo);
        info.setClassId(clazz.getId());
        info.setClassName(clazz.getClassName());
        info.setGrade(clazz.getGrade());
        studentInfoMapper.insert(info);

        result.addSuccess();
    }

    private List<String> readLines(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "导入文件不能为空");
        }
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String raw;
            while ((raw = reader.readLine()) != null) {
                String line = raw.trim();
                // 空行与 # 开头的注释/表头行不计入
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "文件读取失败: " + e.getMessage());
        }
        return lines;
    }

    private void refreshStudentCount(Long classId) {
        Long count = studentInfoMapper.selectCount(Wrappers.<SysStudentInfo>lambdaQuery()
                .eq(SysStudentInfo::getClassId, classId));
        ClassInfo update = new ClassInfo();
        update.setId(classId);
        update.setStudentCount(count.intValue());
        classMapper.updateById(update);
    }

    private Map<Long, User> loadUsers(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    private SysStudentInfo selectByUserId(Long userId) {
        return studentInfoMapper.selectOne(Wrappers.<SysStudentInfo>lambdaQuery()
                .eq(SysStudentInfo::getUserId, userId));
    }

    private void saveOrUpdate(SysStudentInfo entity) {
        if (entity.getId() == null) {
            studentInfoMapper.insert(entity);
        } else {
            studentInfoMapper.updateById(entity);
        }
    }

    private String extractStudentNo(String line) {
        int idx = line.indexOf(',');
        return idx > 0 ? line.substring(0, idx).trim() : line;
    }
}
