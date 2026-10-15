package com.amlve.examapi.service;

import com.amlve.common.request.Result;
import com.amlve.examapi.dto.response.QuestionBaseResponse;
import com.amlve.examapi.dto.response.QuestionDetailResponse;

import java.util.List;

public interface ExamQuestionApi {

    Result<QuestionDetailResponse> getQuestionDetail(Long questionId);

    Result<List<QuestionBaseResponse>> getQuestionsByIds(List<Long> questionIds);

    Result<Void> updateStatistics(Long questionId, Boolean accepted);

    Result<List<QuestionBaseResponse>> getQuestionsForPrepare(Long chapterId);
}
