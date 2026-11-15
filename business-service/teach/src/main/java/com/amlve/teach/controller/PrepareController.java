package com.amlve.teach.controller;

import com.amlve.common.request.Result;
import com.amlve.teach.dto.request.PrepareRequest;
import com.amlve.teach.dto.response.PrepareResponse;
import com.amlve.teach.service.TeachPrepareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class PrepareController {

    private final TeachPrepareService teachPrepareService;

    /**
     * 受理备课请求：秒回备课记录ID（status=2 生成中），
     * 教案/PPT/重难点/课后习题/上机实训由后台线程生成，前端凭ID轮询 /api/teach/prepare/record/detail
     */
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/prepare")
    public Result<PrepareResponse> prepare(@Valid @RequestBody PrepareRequest request) {
        return Result.success(teachPrepareService.generateTeachingMaterials(request));
    }
}
