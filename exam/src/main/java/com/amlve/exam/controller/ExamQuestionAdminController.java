package com.amlve.exam.controller;

import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.exam.dto.request.QuestionGenerateRequest;
import com.amlve.exam.dto.request.QuestionSaveRequest;
import com.amlve.exam.enumeration.QuestionDifficultyEnum;
import com.amlve.common.enums.QuestionTypeEnum;
import com.amlve.exam.service.ExamQuestionService;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.exam.util.ExamAuthSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题目管理接口（教师/管理员）：手动新增 / 修改 / 删除 / 管理端列表，以及随机抽题。
 * <p>
 * 类级 {@code @PreAuthorize("isAuthenticated()")} 要求登录；
 * 教师/管理员权限由 {@link ExamAuthSupport#requireTeacherOrAdmin()} 编程式校验。
 */
@RestController
@RequestMapping("/api/exam/question/admin")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ExamQuestionAdminController {

    private final ExamQuestionService questionService;
    private final ExamAuthSupport examAuthSupport;

    /** 手动新增题目 */
    @PostMapping("/save")
    public Result<Long> save(@Valid @RequestBody QuestionSaveRequest request) {
        examAuthSupport.requireTeacherOrAdmin();
        ExamQuestion question = toEntity(request);
        questionService.save(question);
        return Result.success(question.getId());
    }

    /** 修改题目 */
    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody QuestionSaveRequest request) {
        examAuthSupport.requireTeacherOrAdmin();
        if (request.getId() == null) {
            throw new BusinessException("题目ID不能为空");
        }
        ExamQuestion question = toEntity(request);
        question.setId(request.getId());
        questionService.updateById(question);
        return Result.success(null);
    }

    /** 删除题目（逻辑删除） */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        examAuthSupport.requireTeacherOrAdmin();
        questionService.removeById(id);
        return Result.success(null);
    }

    /** 分页查询题目列表（管理端） */
    @GetMapping("/list")
    public Result<PageResult<ExamQuestion>> list(@RequestParam(required = false) Long chapterId,
                                                 @RequestParam(required = false) Integer type,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size) {
        examAuthSupport.requireTeacherOrAdmin();
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

    /** 查询单题（管理端） */
    @GetMapping("/{id}")
    public Result<ExamQuestion> get(@PathVariable Long id) {
        examAuthSupport.requireTeacherOrAdmin();
        return Result.success(questionService.getById(id));
    }

    /** 按章节随机抽取题目（不调用智能体） */
    @PostMapping("/random")
    public Result<List<ExamQuestion>> random(@Valid @RequestBody QuestionGenerateRequest request) {
        examAuthSupport.requireTeacherOrAdmin();
        return Result.success(questionService.generate(request));
    }

    /** 将保存请求转换为题目实体，options 序列化为 JSON 字符串 */
    private ExamQuestion toEntity(QuestionSaveRequest request) {
        ExamQuestion question = new ExamQuestion();
        question.setContent(request.getContent());
        question.setType(QuestionTypeEnum.fromCode(request.getType()));
        question.setOptions(toOptionsJson(request.getOptions()));
        question.setAnswer(request.getAnswer());
        question.setAnalysis(request.getAnalysis());
        question.setChapterId(request.getChapterId());
        question.setDifficulty(QuestionDifficultyEnum.fromCode(request.getDifficulty()));
        question.setScore(request.getScore() == null ? 5 : request.getScore());
        question.setRequirements(request.getRequirements());
        question.setHints(request.getHints());
        question.setKnowledgePointIds(request.getKnowledgePointIds());
        question.setSolutionOutline(request.getSolutionOutline());
        question.setWeight(request.getWeight());
        question.setJudgeMode(request.getJudgeMode());
        question.setTestCases(request.getTestCases());
        question.setCodeFramework(request.getCodeFramework());
        question.setCognitiveLevel(request.getCognitiveLevel());
        return question;
    }

    /** 选项列表序列化为 JSON 字符串，失败时退化为分号拼接 */
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
}
