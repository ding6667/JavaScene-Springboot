package com.amlve.agentscore.controller;

import com.amlve.agentscore.client.ScoreClientImpl;
import com.amlve.agentscoreapi.dto.request.ScoreEvaluateRequest;
import com.amlve.agentscoreapi.dto.response.ScoreEvaluateResponse;
import com.amlve.agentscoreapi.service.ScoreAgentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ScoreAgentClient 的 HTTP 提供端：Feign 客户端按同一接口调用，方法映射继承自接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/agent/score")
public class ScoreAgentClientController implements ScoreAgentClient {

    private final ScoreClientImpl scoreAgentClient;

    @Override
    public ScoreEvaluateResponse evaluate(ScoreEvaluateRequest request) {
        return scoreAgentClient.evaluate(request);
    }
}
