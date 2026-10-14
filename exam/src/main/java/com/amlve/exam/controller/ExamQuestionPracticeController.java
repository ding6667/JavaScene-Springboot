package com.amlve.exam.controller;

import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.exam.service.ExamQuestionPracticeService;
import com.amlve.exam.util.ExamAuthSupport;
import com.amlve.examapi.dto.request.PracticeQueryRequest;
import com.amlve.examapi.dto.response.PracticeDetailVO;
import com.amlve.examapi.dto.response.PracticeItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 刷题接口（学生）：刷题列表与详情，响应不含答案与解析。
 */
@RestController
@RequestMapping("/api/exam/question/practice")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ExamQuestionPracticeController {

    private final ExamQuestionPracticeService practiceService;
    private final ExamAuthSupport examAuthSupport;

    /** 刷题列表（不含答案） */
    @GetMapping("/list")
    public Result<PageResult<PracticeItemVO>> list(PracticeQueryRequest request) {
        Long userId = examAuthSupport.requireLogin();
        return Result.success(practiceService.listPractice(userId, request));
    }

    /** 刷题详情（不含答案，仅下发示例用例） */
    @GetMapping("/detail/{id}")
    public Result<PracticeDetailVO> detail(@PathVariable Long id) {
        examAuthSupport.requireLogin();
        return Result.success(practiceService.getPracticeDetail(id));
    }
}
