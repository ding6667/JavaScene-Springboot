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

    /** 根据章节ID与生成开关生成备课材料 */
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/prepare")
    public Result<PrepareResponse> prepare(@Valid @RequestBody PrepareRequest request) {
        // 组装备课智能体请求：透传章节ID与各生成开关（DTO 同名，api 侧使用全限定名）
        com.amlve.agentprepareapi.dto.request.PrepareRequest apiRequest =
                new com.amlve.agentprepareapi.dto.request.PrepareRequest();
        apiRequest.setChapterId(request.getChapterId());
        apiRequest.setPptx(request.getPptx());
        apiRequest.setLessonPlan(request.getLessonPlan());
        apiRequest.setHomework(request.getHomework());
        apiRequest.setLab(request.getLab());
        apiRequest.setKeyPoints(request.getKeyPoints());
        apiRequest.setUserInstruction(request.getUserInstruction());
        com.amlve.agentprepareapi.dto.response.PrepareResponse apiResponse = prepareAgentClient.generate(apiRequest);

        // 组装响应：回显章节ID、教案内容与生成时间
        PrepareResponse response = new PrepareResponse();
        response.setChapterId(request.getChapterId());
        response.setTeachingPlan(apiResponse.getLessonPlan());
        response.setGenerateTime(LocalDateTime.now());
        return Result.success(response);
    }
}
