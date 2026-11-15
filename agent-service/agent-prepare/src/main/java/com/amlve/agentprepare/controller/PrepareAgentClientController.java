package com.amlve.agentprepare.controller;

import com.amlve.agentprepare.client.PrepareAgentClientImpl;
import com.amlve.agentprepareapi.dto.request.PrepareRequest;
import com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO;
import com.amlve.agentprepareapi.dto.response.PrepareResponse;
import com.amlve.agentprepareapi.service.PrepareAgentClient;
import com.amlve.common.request.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PrepareAgentClient 的 HTTP 提供端：Feign 客户端按同一接口调用，方法映射继承自接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/agent/prepare")
public class PrepareAgentClientController implements PrepareAgentClient {

    private final PrepareAgentClientImpl prepareAgentClient;

    @Override
    public Result<PrepareResponse> generate(PrepareRequest request, TeachingMaterialDTO teachingMaterialDTO) {
        return prepareAgentClient.generate(request, teachingMaterialDTO);
    }
}
