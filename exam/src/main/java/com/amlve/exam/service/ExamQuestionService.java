package com.amlve.exam.service;

import com.amlve.exam.dto.request.QuestionGenerateRequest;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;

public interface ExamQuestionService extends IService<ExamQuestion> {

    /**
     * 按章节随机抽取题目
     */
    List<ExamQuestion> generate(QuestionGenerateRequest request);
}