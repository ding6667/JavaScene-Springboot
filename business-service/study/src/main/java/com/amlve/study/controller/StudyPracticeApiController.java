package com.amlve.study.controller;

import com.amlve.common.request.Result;
import com.amlve.study.service.impl.StudyPracticeApiImpl;
import com.amlve.studyapi.dto.response.PracticeStatusDTO;
import com.amlve.studyapi.service.StudyPracticeApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * StudyPracticeApi 的 HTTP 提供端：Feign 客户端按同一接口调用，方法映射继承自接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/study/practice")
public class StudyPracticeApiController implements StudyPracticeApi {

    private final StudyPracticeApiImpl studyPracticeApi;

    @Override
    public Result<Map<Long, PracticeStatusDTO>> getQuestionStatusBatch(Long userId, List<Long> questionIds) {
        return studyPracticeApi.getQuestionStatusBatch(userId, questionIds);
    }

    @Override
    public Result<PracticeStatusDTO> getQuestionStatus(Long userId, Long questionId) {
        return studyPracticeApi.getQuestionStatus(userId, questionId);
    }
}
