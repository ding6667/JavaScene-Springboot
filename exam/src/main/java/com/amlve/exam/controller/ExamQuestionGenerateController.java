package com.amlve.exam.controller;

import com.amlve.common.request.Result;
import com.amlve.exam.service.ExamQuestionGenerateService;
import com.amlve.exam.util.ExamAuthSupport;
import com.amlve.examapi.dto.request.QuestionGenerateBizRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 出题接口（教师/管理员）：调用出题智能体生成题目并落库。
 */
@Slf4j
@RestController
@RequestMapping("/api/exam/question/generate")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ExamQuestionGenerateController {

    private final ExamQuestionGenerateService generateService;
    private final ExamAuthSupport examAuthSupport;

    /**
     * AI 生成题目
     *
     * @return 1=成功，0=失败
     */
    @PostMapping
    public Result<Integer> generate(@RequestBody QuestionGenerateBizRequest request) {
        examAuthSupport.requireTeacherOrAdmin();
        log.info("AI 出题: chapterId={}, difficultyConfigId={}, count={}",
                request.getChapterId(), request.getDifficultyConfigId(), request.getCount());
        return Result.success(generateService.generate(request));
    }
}
