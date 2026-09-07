package com.amlve.teach.controller;

import com.amlve.common.request.Result;
import com.amlve.teach.dto.ClassStatsResponse;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 班级学情接口（教师端）。
 * TODO 待study模块负责人在study-api中定义进度查询接口后，
 *  通过 study-api 接口跨模块调用获取真实数据，替换当前占位数据。
 */
@RestController
@RequestMapping("/api/teach/class")
public class ClassAnalysisController {

    /** 查询班级学情：总人数 + 平均进度 */
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/stats")
    public Result<ClassStatsResponse> stats(@RequestParam(required = false) Long classId) {
        ClassStatsResponse response = new ClassStatsResponse();
        // 占位数据：study模块的进度查询接口就绪后替换为真实调用
        response.setTotalStudents(0L);
        response.setAverageProgress(0.0);
        response.setMessage("学情功能待study模块接入，当前为占位数据");
        return Result.success(response);
    }
}
