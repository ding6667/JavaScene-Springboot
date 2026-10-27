package com.amlve.system.controller;

import com.amlve.common.request.Result;
import com.amlve.system.dto.request.ClassSaveRequest;
import com.amlve.system.dto.response.ClassListResponse;
import com.amlve.system.dto.response.StudentListItemResponse;
import com.amlve.system.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班级领域接口。班级与学情不是「用户」概念，故独立挂 /api/class 而非 /api/user/class
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/class")
@PreAuthorize("isAuthenticated()")
public class ClassController {

    private final ClassService classService;

    /**
     * 班级列表，支持按课程筛选。
     * 数据权限：ADMIN 全量，TEACHER 仅自己任课的班，STUDENT 仅自己所在班
     */
    @GetMapping("/list")
    public Result<List<ClassListResponse>> list(@RequestParam(required = false) Long courseId) {
        return Result.success(classService.list(courseId));
    }

    /**
     * 按 classId 查班级学生列表。教师只能查自己带的班，学生只能查自己班
     */
    @GetMapping("/students")
    public Result<List<StudentListItemResponse>> students(@RequestParam Long classId) {
        return Result.success(classService.students(classId));
    }

    /**
     * 新增班级
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> create(@Valid @RequestBody ClassSaveRequest request) {
        return Result.success(classService.create(request));
    }

    /**
     * 更新班级，幂等
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ClassSaveRequest request) {
        classService.update(id, request);
        return Result.success(null);
    }

    /**
     * 删除班级，班内仍有学生时拒绝
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        classService.delete(id);
        return Result.success(null);
    }
}
