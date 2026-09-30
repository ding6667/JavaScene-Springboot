package com.amlve.agentprepareapi.service;


import com.amlve.agentprepareapi.dto.request.PrepareRequest;
import com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO;
import com.amlve.agentprepareapi.dto.response.PrepareResponse;
import com.amlve.common.request.Result;


public interface PrepareAgentClient {
    /**
     * 生成备课材料
     * @param request 备课请求（含章节ID、五个开关）
     * @return 备课结果（含教案、PPT、重难点、作业、实验）
     */
    Result<PrepareResponse> generate(PrepareRequest request , TeachingMaterialDTO teachingMaterialDTO);
}
