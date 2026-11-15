package com.amlve.exam.service.impl;

import com.amlve.exam.dto.request.QuestionAiGenerateRequest;
import com.amlve.exam.service.ExamQuestionGenerateService;
import com.amlve.examapi.dto.request.QuestionGenerateBizRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AI 出题服务实现：把跨模块业务请求转换为内部出题请求，交由出题编排完成。
 */
@Service
@RequiredArgsConstructor
public class ExamQuestionGenerateServiceImpl implements ExamQuestionGenerateService {

    private final ExamQuestionGenerator questionGenerator;

    @Override
    public Integer generate(QuestionGenerateBizRequest request) {
        QuestionAiGenerateRequest aiRequest = new QuestionAiGenerateRequest();
        aiRequest.setChapterId(request.getChapterId());
        aiRequest.setChapterName(request.getChapterName());
        aiRequest.setDifficultyConfigId(request.getDifficultyConfigId());
        aiRequest.setQuestionType(request.getQuestionType());
        aiRequest.setCount(request.getCount());
        aiRequest.setUserInstruction(request.getUserInstruction());
        return questionGenerator.generate(aiRequest);
    }
}
