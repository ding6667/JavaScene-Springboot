package com.amlve.exam.service;

import com.amlve.examapi.dto.request.QuestionGenerateBizRequest;

/**
 * AI 出题服务：调用出题智能体生成题目并落库。
 */
public interface ExamQuestionGenerateService {

    /**
     * AI 生成题目
     *
     * @param request 出题业务请求
     * @return 1=成功，0=失败
     */
    Integer generate(QuestionGenerateBizRequest request);
}
