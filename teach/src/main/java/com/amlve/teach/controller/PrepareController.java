package com.amlve.teach.controller;

import com.amlve.agentprepareapi.PrepareAgentClient;
import com.amlve.common.request.Result;
import com.amlve.teach.dto.request.PrepareRequest;
import com.amlve.teach.dto.response.PrepareResponse;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 备课智能体接口（教师端）
 */
@RestController
@RequestMapping("/api/agent/teach")
public class PrepareController {

    /** 通过 agent-prepare-api 接口注入，不直接依赖实现类 */
    @Resource
    private PrepareAgentClient prepareAgentClient;

    /** 根据章节ID生成教案 */
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/prepare")
    public Result<PrepareResponse> prepare(@Valid @RequestBody PrepareRequest request) {
        // 将章节ID组装为备课提示词，交由备课智能体生成教案
        String lessonPrompt = "第" + request.getChapterId() + "章";
        String teachingPlan = prepareAgentClient.getLessonText(lessonPrompt);

        // 组装响应：回显章节ID、教案内容与生成时间
        PrepareResponse response = new PrepareResponse();
        response.setChapterId(request.getChapterId());
        response.setTeachingPlan(teachingPlan);
        response.setGenerateTime(LocalDateTime.now());
        return Result.success(response);
    }
}
