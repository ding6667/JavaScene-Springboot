package com.amlve.studyapi.service;

import com.amlve.common.request.Result;
import com.amlve.studyapi.dto.response.PracticeStatusResponse;

import java.util.List;
import java.util.Map;

public interface StudyPracticeService {

    /**
     * 批量查学生题目状态
     * 【exam 调】拼装刷题列表时，批量拿 done / correct / attemptCount / lastAnswer / favorited
     */
    Result<Map<Long, PracticeStatusResponse>> getQuestionStatusBatch(Long userId, List<Long> questionIds);

    /**
     * 查单题状态
     * 【exam 调】刷题详情页拼装个人状态
     */
    Result<PracticeStatusResponse> getQuestionStatus(Long userId, Long questionId);
}
