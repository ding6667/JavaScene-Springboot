package com.amlve.examapi.service;

import com.amlve.common.request.Result;
import com.amlve.examapi.dto.response.QuestionBaseDTO;
import com.amlve.examapi.dto.response.QuestionDetailDTO;

import java.util.List;

/**
 * 题库 API（供其他模块调用）
 */
public interface ExamQuestionApi {

    /**
     * 查题目完整详情（含答案、测试用例）
     * 仅供内部模块调用，不暴露给前端
     */
    Result<QuestionDetailDTO> getQuestionDetail(Long questionId);

    /**
     * 批量查题目基本信息（不含答案）
     */
    Result<List<QuestionBaseDTO>> getQuestionsByIds(List<Long> questionIds);

    /**
     * 更新题目统计（提交次数、通过次数、通过率）
     */
    Result<Void> updateStatistics(Long questionId, Boolean accepted);

    /**
     * 按章节查题库题目列表
     * 【teach 调】备课生成时，拿到本章题目供 AI 推荐，目前逻辑为从题库对应章节随机抽取五十题
     */
    Result<List<QuestionBaseDTO>> getQuestionsByChapter(Long chapterId);
}
