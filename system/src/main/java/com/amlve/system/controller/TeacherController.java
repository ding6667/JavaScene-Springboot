package com.amlve.system.controller;

import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.system.dto.request.TeacherInfoUpdateRequest;
import com.amlve.system.dto.response.TeacherInfoResponse;
import com.amlve.system.service.TeacherInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师自助完善履历信息
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    private final TeacherInfoService teacherInfoService;

    /**
     * 查询自己的教师扩展信息
     */
    @GetMapping("/info")
    public Result<TeacherInfoResponse> info() {
        return Result.success(teacherInfoService.getByUserId(SecurityUtils.getUserId()));
    }

    /**
     * 完善自己的教师扩展信息，工号首次填写后不可改
     */
    @PutMapping("/info/update")
    public Result<Void> update(@Valid @RequestBody TeacherInfoUpdateRequest request) {
        teacherInfoService.updateSelf(SecurityUtils.getUserId(), request);
        return Result.success(null);
    }
}
