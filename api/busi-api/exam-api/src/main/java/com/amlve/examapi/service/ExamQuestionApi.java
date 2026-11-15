package com.amlve.examapi.service;

import com.amlve.common.request.Result;
import com.amlve.common.web.feign.FeignConfig;
import com.amlve.examapi.dto.response.QuestionBaseDTO;
import com.amlve.examapi.dto.response.QuestionDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 题库 API（供其他模块调用）
 */
@FeignClient(name = "exam-service", contextId = "examQuestionApi", path = "/internal/exam/question", configuration = FeignConfig.class)
public interface ExamQuestionApi {

    /**
     * 查题目完整详情（含答案、测试用例）
     * 仅供内部模块调用，不暴露给前端
     */
    @PostMapping("/getQuestionDetail")
    Result<QuestionDetailResponse> getQuestionDetail(@RequestParam("questionId") Long questionId);

    /**
     * 批量查题目基本信息（不含答案）
     */
    @PostMapping("/getQuestionsByIds")
    Result<List<QuestionBaseDTO>> getQuestionsByIds(@RequestBody List<Long> questionIds);

    /**
     * 更新题目统计（提交次数、通过次数、通过率）
     */
    @PostMapping("/updateStatistics")
    Result<Void> updateStatistics(@RequestParam("questionId") Long questionId, @RequestParam("accepted") Boolean accepted);

    /**
     * 按章节查题库题目列表
     * 【teach 调】备课生成时，拿到本章题目供 AI 推荐，目前逻辑为从题库对应章节随机抽取五十题
     */
    @PostMapping("/getQuestionsByChapter")
    Result<List<QuestionBaseDTO>> getQuestionsByChapter(@RequestParam("chapterId") Long chapterId);
}
