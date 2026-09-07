package com.amlve.agentscoreapi;

import com.amlve.agentscoreapi.dto.request.ScoreEvaluateRequest;
import com.amlve.agentscoreapi.dto.response.ScoreEvaluateResponse;

public interface ScoreAgentClient {
    /**
     * 调用 Dify 批改工作流
     */
    ScoreEvaluateResponse evaluate(ScoreEvaluateRequest request);
}
