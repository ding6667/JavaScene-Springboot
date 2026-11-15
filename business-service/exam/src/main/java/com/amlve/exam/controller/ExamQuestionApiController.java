package com.amlve.exam.controller;

import com.amlve.common.request.Result;
import com.amlve.exam.service.impl.ExamQuestionApiImpl;
import com.amlve.examapi.dto.response.QuestionBaseDTO;
import com.amlve.examapi.dto.response.QuestionDetailResponse;
import com.amlve.examapi.service.ExamQuestionApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ExamQuestionApi 的 HTTP 提供端：Feign 客户端按同一接口调用，方法映射继承自接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/exam/question")
public class ExamQuestionApiController implements ExamQuestionApi {

    private final ExamQuestionApiImpl examQuestionApi;

    @Override
    public Result<QuestionDetailResponse> getQuestionDetail(Long questionId) {
        return examQuestionApi.getQuestionDetail(questionId);
    }

    @Override
    public Result<List<QuestionBaseDTO>> getQuestionsByIds(List<Long> questionIds) {
        return examQuestionApi.getQuestionsByIds(questionIds);
    }

    @Override
    public Result<Void> updateStatistics(Long questionId, Boolean accepted) {
        return examQuestionApi.updateStatistics(questionId, accepted);
    }

    @Override
    public Result<List<QuestionBaseDTO>> getQuestionsByChapter(Long chapterId) {
        return examQuestionApi.getQuestionsByChapter(chapterId);
    }
}
