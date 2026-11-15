package com.amlve.agentprepareapi.service;


import com.amlve.agentprepareapi.dto.request.PrepareRequest;
import com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO;
import com.amlve.agentprepareapi.dto.response.PrepareResponse;
import com.amlve.common.request.Result;
import com.amlve.common.web.feign.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "agent-prepare-service", path = "/internal/agent/prepare", configuration = FeignConfig.class)
public interface PrepareAgentClient {
    /**
     * 生成备课材料
     * @param request 备课请求（含章节ID、五个开关）
     * @return 备课结果（含教案、PPT、重难点、作业、实验）
     */
    @PostMapping("/generate")
    // teachingMaterialDTO 为第二个对象入参，摊平为查询参数传递（一个方法只能有一个 @RequestBody）
    Result<PrepareResponse> generate(@RequestBody PrepareRequest request , @SpringQueryMap TeachingMaterialDTO teachingMaterialDTO);
}
