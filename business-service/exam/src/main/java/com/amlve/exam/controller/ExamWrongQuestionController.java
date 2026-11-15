package com.amlve.exam.controller;

import com.amlve.common.request.Result;
import com.amlve.exam.dto.response.WrongQuestionRetryVO;
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
 * 错题本接口（学生）：查看本人错题、重做、标记已掌握。
 * <p>
 * 列表/重做返回的题面都取自错题记录指向的「题目快照」，
 * 题库后续修题不影响历史错题，也不会把标准答案/测试用例下发出去。
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

    /**
     * 错题重做页面：按错题记录ID取「当时那一版题」（不含答案/解析/用例）。
     * <p>
     * 提交作答走 {@code POST /api/study/practice/submit}，带上 {@code wrongQuestionId}，
     * 由 study 按同一份快照的判题包判分，不会用题库最新用例。
     */
    @GetMapping("/{id}/retry")
    public Result<WrongQuestionRetryVO> retry(@PathVariable Long id) {
        Long userId = examAuthSupport.requireLogin();
        return Result.success(wrongQuestionService.getRetryView(userId, id));
    }

    /**
     * 标记单条错题已掌握（推荐：同一题可能有多个版本，按记录ID定位最准确）。
     */
    @PutMapping("/mastered/record/{id}")
    public Result<Void> markMasteredById(@PathVariable Long id) {
        Long userId = examAuthSupport.requireLogin();
        wrongQuestionService.markMastered(userId, id);
        return Result.success(null);
    }

    /**
     * 标记某道题的全部版本错题已掌握（兼容旧前端：按题库题目ID调用）。
     */
    @PutMapping("/mastered/{questionId}")
    public Result<Integer> markMastered(@PathVariable Long questionId) {
        Long userId = examAuthSupport.requireLogin();
        return Result.success(wrongQuestionService.markMasteredByQuestion(userId, questionId));
    }
}
