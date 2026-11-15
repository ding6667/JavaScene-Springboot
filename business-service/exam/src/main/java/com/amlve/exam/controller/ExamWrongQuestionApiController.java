package com.amlve.exam.controller;

import com.amlve.common.request.Result;
import com.amlve.exam.service.impl.ExamWrongQuestionApiImpl;
import com.amlve.examapi.dto.request.WrongQuestionArchiveRequest;
import com.amlve.examapi.dto.response.WrongQuestionRetryDTO;
import com.amlve.examapi.service.ExamWrongQuestionApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ExamWrongQuestionApi 的 HTTP 提供端：Feign 客户端按同一接口调用，方法映射继承自接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/exam/wrong-question")
public class ExamWrongQuestionApiController implements ExamWrongQuestionApi {

    private final ExamWrongQuestionApiImpl examWrongQuestionApi;

    @Override
    public Result<Void> archiveWrongQuestion(Long userId, WrongQuestionArchiveRequest request) {
        return examWrongQuestionApi.archiveWrongQuestion(userId, request);
    }

    @Override
    public Result<Integer> archiveWrongQuestions(Long userId, List<WrongQuestionArchiveRequest> requests) {
        return examWrongQuestionApi.archiveWrongQuestions(userId, requests);
    }

    @Override
    public Result<WrongQuestionRetryDTO> getRetryPayload(Long userId, Long wrongQuestionId) {
        return examWrongQuestionApi.getRetryPayload(userId, wrongQuestionId);
    }
}
