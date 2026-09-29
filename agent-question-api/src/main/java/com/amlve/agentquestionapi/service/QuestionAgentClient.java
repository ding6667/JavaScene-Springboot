package com.amlve.agentquestionapi.service;

import com.amlve.agentquestionapi.dto.request.QuestionGenerateRequest;
import com.amlve.agentquestionapi.dto.response.QuestionGenerateResponse;
import com.amlve.common.request.Result;

public interface QuestionAgentClient {
    Result<QuestionGenerateResponse> generate(QuestionGenerateRequest request);
}

