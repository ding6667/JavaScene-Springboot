package com.amlve.system.controller;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.system.dto.request.StudentInfoUpdateRequest;
import com.amlve.system.dto.request.StudentQueryRequest;
import com.amlve.system.dto.response.BatchImportResponse;
import com.amlve.system.dto.response.StudentInfoResponse;
import com.amlve.system.dto.response.StudentListItemResponse;
import com.amlve.system.service.RoleService;
import com.amlve.system.service.StudentInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学生列表、批量导入与学生自助完善学籍信息
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/student")
@PreAuthorize("isAuthenticated()")
public class StudentController {

    private final StudentInfoService studentInfoService;

    /**
     * 分页查询学生列表。教师只能查自己带的班，管理员不限
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<PageResult<StudentListItemResponse>> list(StudentQueryRequest request) {
        return Result.success(studentInfoService.pageStudents(resolveClassScope(request), request));
    }

    /**
     * 批量导入学生。txt / md 文件，一行一名学生：学号,姓名[,初始密码]
     */
    @PostMapping("/batch-add")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<BatchImportResponse> batchAdd(@RequestParam Long classId,
                                                @RequestParam("file") MultipartFile file) {
        return Result.success(studentInfoService.batchAdd(classId, file));
    }

    /**
     * 下载导入模板。没有这个接口，"格式自行定义"就等于让管理员猜
     */
    @GetMapping("/import-template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importTemplate() {
        return download(studentInfoService.importTemplate(), "student-import-template.txt",
                MediaType.TEXT_PLAIN);
    }

    /**
     * 导出学生名单 CSV，可见范围与 /list 完全一致
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<String> export(StudentQueryRequest request) {
        String csv = studentInfoService.exportCsv(resolveClassScope(request), request.getKeyword());
        return download(csv, "students.csv", MediaType.parseMediaType("text/csv;charset=UTF-8"));
    }

    /**
     * 查询自己的学生扩展信息。userId 只从 token 取，不接受参数，否则就是 IDOR
     */
    @GetMapping("/info")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<StudentInfoResponse> info() {
        return Result.success(studentInfoService.getByUserId(SecurityUtils.getUserId()));
    }

    /**
     * 完善自己的学生扩展信息，学号首次填写后不可改
     */
    @PutMapping("/info/update")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> update(@Valid @RequestBody StudentInfoUpdateRequest request) {
        studentInfoService.updateSelf(SecurityUtils.getUserId(), request);
        return Result.success(null);
    }

    private ResponseEntity<String> download(String body, String filename, MediaType mediaType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(body);
    }

    /**
     * 把调用者身份翻译成班级范围：null 表示不限，空集合表示无可见班级
     */
    private List<Long> resolveClassScope(StudentQueryRequest request) {
        Long classId = request.getClassId();
        if (SecurityUtils.hasRole(RoleService.ADMIN)) {
            return classId == null ? null : List.of(classId);
        }
        List<Long> own = studentInfoService.listClassIdsByTeacher(SecurityUtils.getUserId());
        if (classId == null) {
            return own;
        }
        if (!own.contains(classId)) {
            throw new BusinessException(ErrorCodeConstant.ACCESS_DENIED, "该班级不属于当前教师");
        }
        return List.of(classId);
    }
}
