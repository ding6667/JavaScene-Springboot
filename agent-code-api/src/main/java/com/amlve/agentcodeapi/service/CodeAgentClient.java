package com.amlve.agentcodeapi.service;

import com.amlve.agentcodeapi.dto.request.CodeDebugRequest;
import com.amlve.agentcodeapi.dto.response.CodeDebugResponse;
import com.amlve.common.request.Result;

/**
 * 代码调试智能体 API
 */
public interface CodeAgentClient {

    /**
     * 调用代码调试智能体
     *
     * @param request 调试请求（含代码、题目信息、错误信息）
     * @return 调试响应（含错误原因、优化建议、解题思路）
     */
    Result<CodeDebugResponse> debug(CodeDebugRequest request);
}