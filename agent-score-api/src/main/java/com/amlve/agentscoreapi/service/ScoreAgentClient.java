package com.amlve.agentscoreapi.service;

import com.amlve.agentscoreapi.dto.request.ScoreEvaluateRequest;
import com.amlve.agentscoreapi.dto.response.ScoreEvaluateResponse;

/**
 * 评分智能体 API。
 */
public interface ScoreAgentClient {

    /**
     * 调用 Dify 批改工作流
     */
    ScoreEvaluateResponse evaluate(ScoreEvaluateRequest request);
}
