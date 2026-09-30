package com.amlve.exam.controller;

import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.exam.dto.request.QuestionAiGenerateRequest;
import com.amlve.exam.dto.request.QuestionGenerateRequest;
import com.amlve.exam.dto.request.QuestionSaveRequest;
import com.amlve.exam.enumeration.QuestionDifficultyEnum;
import com.amlve.exam.enumeration.QuestionTypeEnum;
import com.amlve.exam.service.ExamQuestionService;
import com.amlve.exam.service.QuestionAgentImpl;
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
 * 题库管理接口：增删改查、随机抽题、AI 出题。
 * <p>
 * 接口级 {@code @PreAuthorize("isAuthenticated()")} 要求登录；管理类操作的角色校验
 * （教师/管理员）因 JWT 无角色，统一经 {@link ExamAuthSupport} 编程式查库完成。
 */
@RestController
@RequestMapping("/api/exam/question")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ExamQuestionController {

    private final ExamQuestionService questionService;
    private final QuestionAgentImpl questionAgent;
    private final ExamAuthSupport examAuthSupport;

    /** 新增题目（教师/管理员） */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody QuestionSaveRequest request) {
        examAuthSupport.requireTeacherOrAdmin();
        ExamQuestion question = toEntity(request);
        questionService.save(question);
        return Result.success(question.getId());
    }

    /** 更新题目（教师/管理员） */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody QuestionSaveRequest request) {
        examAuthSupport.requireTeacherOrAdmin();
        ExamQuestion question = toEntity(request);
        question.setId(id);
        questionService.updateById(question);
        return Result.success(null);
    }

    /** 删除题目（教师/管理员） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        examAuthSupport.requireTeacherOrAdmin();
        questionService.removeById(id);
        return Result.success(null);
    }

    /** 查询题目详情（教师/管理员） */
    @GetMapping("/{id}")
    public Result<ExamQuestion> get(@PathVariable Long id) {
        examAuthSupport.requireTeacherOrAdmin();
        return Result.success(questionService.getById(id));
    }

    /** 分页查询题目（教师/管理员） */
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

    /** 按章节随机抽取题目（教师/管理员） */
    @PostMapping("/random")
    public Result<List<ExamQuestion>> generate(@Valid @RequestBody QuestionGenerateRequest request) {
        examAuthSupport.requireTeacherOrAdmin();
        return Result.success(questionService.generate(request));
    }

    /** AI 出题：调用出题智能体生成并落库，返回 1 成功 / 0 失败（教师/管理员） */
    @PostMapping("/generate")
    public Result<Integer> generateAi(@Valid @RequestBody QuestionAiGenerateRequest request) {
        examAuthSupport.requireTeacherOrAdmin();
        return Result.success(questionAgent.generate(request));
    }

    /**
     * 将保存请求转换为题目实体
     *
     * @param request 题目保存请求
     * @return 题目实体
     */
    private ExamQuestion toEntity(QuestionSaveRequest request) {
        ExamQuestion question = new ExamQuestion();
        question.setContent(request.getContent());
        question.setType(QuestionTypeEnum.fromCode(request.getType()));
        question.setOptions(request.getOptions());
        question.setAnswer(request.getAnswer());
        question.setChapterId(request.getChapterId());
        question.setDifficulty(QuestionDifficultyEnum.fromCode(request.getDifficulty()));
        question.setScore(request.getScore() == null ? 5 : request.getScore());
        return question;
    }
}
