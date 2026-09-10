package com.amlve.agentquestionapi.service;

import com.amlve.agentquestionapi.dto.request.QuestionGenerateRequest;
import com.amlve.agentquestionapi.dto.response.QuestionGenerateResponse;

public interface QuestionAgentClient {
    QuestionGenerateResponse generate(QuestionGenerateRequest request);
}

