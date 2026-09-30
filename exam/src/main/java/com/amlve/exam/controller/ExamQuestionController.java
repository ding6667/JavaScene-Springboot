package com.amlve.exam.controller;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.exam.dto.request.QuestionAiGenerateRequest;
import com.amlve.exam.dto.request.QuestionGenerateRequest;
import com.amlve.exam.dto.request.QuestionSaveRequest;
import com.amlve.exam.enumeration.QuestionDifficultyEnum;
import com.amlve.exam.enumeration.QuestionTypeEnum;
import com.amlve.exam.service.ExamQuestionService;
import com.amlve.exam.service.impl.QuestionAgentImpl;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.systemapi.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
// TODO 建议将checkTeacherOrAdmin改为@PreAuthorize("hasRole('TEACHER')")或者@PreAuthorize("hasRole('ADMIN')")或者@PreAuthorize("hasRole('STUDENT')")
// TODO 缺少javadoc注释

@RestController
@RequestMapping("/api/exam/question")
@RequiredArgsConstructor
public class ExamQuestionController {

    private final ExamQuestionService questionService;
    private final QuestionAgentImpl questionAgent;
    private final UserService userService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody QuestionSaveRequest request) {
        checkTeacherOrAdmin();
        ExamQuestion question = toEntity(request);
        questionService.save(question);
        return Result.success(question.getId());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody QuestionSaveRequest request) {
        checkTeacherOrAdmin();
        ExamQuestion question = toEntity(request);
        question.setId(id);
        questionService.updateById(question);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkTeacherOrAdmin();
        questionService.removeById(id);
        return Result.success(null);
    }

    @GetMapping("/{id}")
    public Result<ExamQuestion> get(@PathVariable Long id) {
        checkTeacherOrAdmin();
        return Result.success(questionService.getById(id));
    }

    // TODO 丰富查询参数
    @GetMapping("/list")
    public Result<PageResult<ExamQuestion>> list(@RequestParam(required = false) Long chapterId,
                                                 @RequestParam(required = false) Integer type,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size) {
        checkTeacherOrAdmin();
        LambdaQueryWrapper<ExamQuestion> wrapper = Wrappers.<ExamQuestion>lambdaQuery()
                .eq(chapterId != null, ExamQuestion::getChapterId, chapterId)
                .eq(type != null, ExamQuestion::getType, QuestionTypeEnum.fromCode(type))
                .orderByDesc(ExamQuestion::getCreateTime);
        Page<ExamQuestion> pageData = questionService.page(new Page<>(page, size), wrapper);
        PageResult<ExamQuestion> result = new PageResult<>();
        result.setRecords(pageData.getRecords());
        result.setTotal(pageData.getTotal());
        result.setCurrent(pageData.getCurrent());
        result.setSize(pageData.getSize());
        return Result.success(result);
    }

    @PostMapping("/random")
    public Result<List<ExamQuestion>> generate(@Valid @RequestBody QuestionGenerateRequest request) {
        checkTeacherOrAdmin();
        return Result.success(questionService.generate(request));
    }

    @PostMapping("/generate")
    public Result<Integer> generateAi(@Valid @RequestBody QuestionAiGenerateRequest request) {
        checkTeacherOrAdmin();
        return Result.success(questionAgent.generate(request));
    }

    private ExamQuestion toEntity(QuestionSaveRequest request) {
        ExamQuestion question = new ExamQuestion();
        question.setContent(request.getContent());
        question.setType(QuestionTypeEnum.fromCode(request.getType()));
        question.setOptions(toOptionsJson(request.getOptions()));
        question.setAnswer(request.getAnswer());
        question.setChapterId(request.getChapterId());
        question.setDifficulty(QuestionDifficultyEnum.fromCode(request.getDifficulty()));
        question.setScore(request.getScore() == null ? 5 : request.getScore());
        question.setCodeFramework(request.getCodeFramework());
        question.setCognitiveLevel(request.getCognitiveLevel());
        return question;
    }

    private String toOptionsJson(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(options);
        } catch (Exception e) {
            return String.join(";", options);
        }
    }

    private void checkTeacherOrAdmin() {
        Long userId = SecurityUtils.getUserId();
        if (!userService.isTeacher(userId) && !userService.isAdmin(userId)) {
            throw new BusinessException(ErrorCodeConstant.ACCESS_DENIED, "权限不足，仅管理员或教师可操作");
        }
    }
}