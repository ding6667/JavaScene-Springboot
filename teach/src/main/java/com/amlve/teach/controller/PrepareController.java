package com.amlve.teach.controller;

import com.amlve.agentprepare.client.PrepareClient;
import com.amlve.common.request.Result;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 备课智能体接口（教师端）
 */
@RestController
@RequestMapping("/api/agent/teach")
public class PrepareController {

    @Resource
    private PrepareClient prepareClient;

    /** 根据章节ID生成教案 */
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/prepare")
    public Result<String> prepare(@RequestParam Long chapterId) {
        return Result.success(prepareClient.generateTeachingPlan(chapterId));
    }
}
