package com.amlve.agentcode.controller;

import com.amlve.agentcode.client.CodeAgentClientImpl;
import com.amlve.agentcodeapi.dto.request.CodeDebugRequest;
import com.amlve.agentcodeapi.dto.response.CodeDebugResponse;
import com.amlve.agentcodeapi.service.CodeAgentClient;
import com.amlve.common.request.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CodeAgentClient 的 HTTP 提供端：Feign 客户端按同一接口调用，方法映射继承自接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/agent/code")
public class CodeAgentClientController implements CodeAgentClient {

    private final CodeAgentClientImpl codeAgentClient;

    @Override
    public Result<CodeDebugResponse> debug(CodeDebugRequest request) {
        return codeAgentClient.debug(request);
    }
}
