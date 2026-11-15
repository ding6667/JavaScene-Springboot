package com.amlve.exam.service;

import com.amlve.common.request.PageResult;
import com.amlve.examapi.dto.request.PracticeQueryRequest;
import com.amlve.examapi.dto.response.PracticeDetailVO;
import com.amlve.examapi.dto.response.PracticeItemVO;

/**
 * 学生刷题服务：刷题列表与详情（不含答案）。
 */
public interface ExamQuestionPracticeService {

    /**
     * 刷题列表（不含答案，拼装公共统计与学生个人状态）
     *
     * @param userId  当前学生ID
     * @param request 查询条件
     * @return 分页结果
     */
    PageResult<PracticeItemVO> listPractice(Long userId, PracticeQueryRequest request);

    /**
     * 刷题详情（不含答案，仅下发示例用例）
     *
     * @param questionId 题目ID
     * @return 题目详情
     */
    PracticeDetailVO getPracticeDetail(Long questionId);
}
