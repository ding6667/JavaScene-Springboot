package com.amlve.agentscoreapi.service;

import com.amlve.agentscoreapi.dto.request.ScoreEvaluateRequest;
import com.amlve.agentscoreapi.dto.response.ScoreEvaluateResponse;
import com.amlve.common.web.feign.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 评分智能体 API。
 */
@FeignClient(name = "agent-score-service", path = "/internal/agent/score", configuration = FeignConfig.class)
public interface ScoreAgentClient {

    /**
     * 调用 Dify 批改工作流
     */
    @PostMapping("/evaluate")
    ScoreEvaluateResponse evaluate(@RequestBody ScoreEvaluateRequest request);
}
