package com.amlve.exam.service;

import com.amlve.exam.dto.request.QuestionGenerateRequest;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;

/**
 * 题库服务：题目持久化与随机抽题。
 */
public interface ExamQuestionService extends IService<ExamQuestion> {

    /**
     * 按章节随机抽取题目（不调用智能体）。
     *
     * @param request 抽题请求（章节、数量，可选题型/难度）
     * @return 随机抽取的题目列表（无题时为空列表）
     */
    List<ExamQuestion> generate(QuestionGenerateRequest request);
}