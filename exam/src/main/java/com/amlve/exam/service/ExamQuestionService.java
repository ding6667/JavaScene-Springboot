package com.amlve.exam.service;

import com.amlve.common.request.PageResult;
import com.amlve.exam.dto.request.QuestionGenerateRequest;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.examapi.dto.request.QuestionAdminQueryRequest;
import com.amlve.examapi.dto.response.QuestionAdminVO;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;

/**
 * 题库服务：题目持久化、随机抽题与管理端列表。
 */
public interface ExamQuestionService extends IService<ExamQuestion> {

    /**
     * 按章节随机抽取题目（不调用智能体）。
     *
     * @param request 抽题请求（章节、数量，可选题型/难度）
     * @return 随机抽取的题目列表（无题时为空列表）
     */
    List<ExamQuestion> generate(QuestionGenerateRequest request);

    /**
     * 管理端题目分页列表（教师/管理员，返回含答案的完整视图）。
     *
     * @param request 查询条件（章节/题型/难度/关键字 + 分页）
     * @return 分页结果
     */
    PageResult<QuestionAdminVO> listForAdmin(QuestionAdminQueryRequest request);
}