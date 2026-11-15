package com.amlve.studyapi.service;

import com.amlve.common.request.Result;
import com.amlve.common.web.feign.FeignConfig;
import com.amlve.studyapi.dto.response.PracticeStatusDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 刷题状态 API（study 模块实现，供 exam 调用）
 */
@FeignClient(name = "study-service", path = "/internal/study/practice", configuration = FeignConfig.class)
public interface StudyPracticeApi {

    /**
     * 批量查学生题目状态
     * 【exam 调】拼装刷题列表时，批量拿 done / correct / attemptCount / lastAnswer / favorited
     */
    @PostMapping("/getQuestionStatusBatch")
    Result<Map<Long, PracticeStatusDTO>> getQuestionStatusBatch(@RequestParam("userId") Long userId, @RequestBody List<Long> questionIds);

    /**
     * 查单题状态
     * 【exam 调】刷题详情页拼装个人状态
     */
    @PostMapping("/getQuestionStatus")
    Result<PracticeStatusDTO> getQuestionStatus(@RequestParam("userId") Long userId, @RequestParam("questionId") Long questionId);
}
