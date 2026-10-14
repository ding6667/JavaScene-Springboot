package com.amlve.exam.controller;

import com.amlve.common.request.Result;
import com.amlve.exam.service.ExamWrongQuestionService;
import com.amlve.exam.util.ExamAuthSupport;
import com.amlve.examapi.dto.response.WrongQuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 错题本接口（学生）：查看本人错题、标记已掌握。
 */
@RestController
@RequestMapping("/api/exam/wrong-question")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ExamWrongQuestionController {

    private final ExamWrongQuestionService wrongQuestionService;
    private final ExamAuthSupport examAuthSupport;

    /** 学生错题本列表 */
    @GetMapping("/list")
    public Result<List<WrongQuestionVO>> list() {
        Long userId = examAuthSupport.requireLogin();
        return Result.success(wrongQuestionService.listWrongQuestions(userId));
    }

    /** 标记错题已掌握 */
    @PutMapping("/mastered/{questionId}")
    public Result<Void> markMastered(@PathVariable Long questionId) {
        Long userId = examAuthSupport.requireLogin();
        wrongQuestionService.markMastered(userId, questionId);
        return Result.success(null);
    }
}
