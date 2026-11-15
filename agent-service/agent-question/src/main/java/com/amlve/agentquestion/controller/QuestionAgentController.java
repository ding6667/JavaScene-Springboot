package com.amlve.agentquestion.controller;

import com.amlve.agentquestion.client.QuestionAgentImpl;
import com.amlve.agentquestionapi.dto.request.QuestionGenerateRequest;
import com.amlve.agentquestionapi.dto.response.QuestionGenerateResponse;
import com.amlve.agentquestionapi.service.QuestionAgent;
import com.amlve.common.request.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * QuestionAgent 的 HTTP 提供端：Feign 客户端按同一接口调用，方法映射继承自接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/agent/question")
public class QuestionAgentController implements QuestionAgent {

    private final QuestionAgentImpl questionAgent;

    @Override
    public Result<QuestionGenerateResponse> generate(QuestionGenerateRequest request) {
        return questionAgent.generate(request);
    }
}
