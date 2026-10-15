package com.amlve.agentquestionapi.service;

import com.amlve.agentquestionapi.dto.request.QuestionGenerateRequest;
import com.amlve.agentquestionapi.dto.response.QuestionGenerateResponse;
import com.amlve.common.request.Result;

/**
 * 出题智能体 API。
 */
public interface QuestionAgent {

    /**
     * 调用出题智能体生成题目
     *
     * @param request 出题请求（含章节、难度、题型、数量等）
     * @return 生成的题目列表（含测试用例、提示、相似题）
     */
    Result<QuestionGenerateResponse> generate(QuestionGenerateRequest request);
}
