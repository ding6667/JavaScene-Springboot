package com.amlve.exam.service;

import com.amlve.examapi.dto.response.WrongQuestionVO;

import java.util.List;

/**
 * 错题归档服务。
 */
public interface ExamWrongQuestionService {

    /**
     * 归档一道错题：不存在则新增，存在则累计错误次数并更新最近答案（重新置为未掌握）
     *
     * @param userId     学生ID
     * @param questionId 题目ID
     * @param lastAnswer 最近一次错误答案
     * @param source     来源：1=作业，2=刷题
     */
    void archive(Long userId, Long questionId, String lastAnswer, Integer source);

    /**
     * 查询学生错题本
     *
     * @param userId 学生ID
     * @return 错题列表
     */
    List<WrongQuestionVO> listWrongQuestions(Long userId);

    /**
     * 标记错题已掌握
     *
     * @param userId     学生ID
     * @param questionId 题目ID
     */
    void markMastered(Long userId, Long questionId);
}
