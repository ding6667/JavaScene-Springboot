package com.amlve.teach.controller;

import com.amlve.common.request.Result;
import com.amlve.teach.dto.request.PrepareRequest;
import com.amlve.teach.dto.response.PrepareResponse;
import com.amlve.teach.service.TeachPrepareService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 备课智能体接口（教师端）
 */
@RestController
@RequestMapping("/api/agent/teach")
public class PrepareController {

    @Resource
    private TeachPrepareService teachPrepareService;

    /** 根据章节ID与生成开关生成备课材料（教案/PPT/重难点/课后习题/上机实训） */
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/prepare")
    public Result<PrepareResponse> prepare(@Valid @RequestBody PrepareRequest request) {
        return Result.success(teachPrepareService.generateTeachingMaterials(request));
    }
}
