package com.amlve.exam.service.impl;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.request.Result;
import com.amlve.exam.enumeration.QuestionDifficultyEnum;
import com.amlve.common.enums.QuestionTypeEnum;
import com.amlve.exam.mapper.ExamQuestionMapper;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.examapi.dto.response.QuestionBaseDTO;
import com.amlve.examapi.dto.response.QuestionDetailDTO;
import com.amlve.examapi.service.ExamQuestionApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/**
 * 题库跨模块 API 实现：供 study / teach 查询题目详情、批量基本信息与回写统计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamQuestionApiImpl implements ExamQuestionApi {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ExamQuestionMapper questionMapper;

    @Override
    public Result<QuestionDetailDTO> getQuestionDetail(Long questionId) {
        ExamQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            return Result.error(ErrorCodeConstant.NOT_FOUND, "题目不存在: " + questionId);
        }
        return Result.success(toDetail(question));
    }

    @Override
    public Result<List<QuestionBaseDTO>> getQuestionsByIds(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Result.success(List.of());
        }
        List<ExamQuestion> questions = questionMapper.selectBatchIds(questionIds);
        return Result.success(questions.stream().map(this::toBase).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateStatistics(Long questionId, Boolean accepted) {
        ExamQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            return Result.error(ErrorCodeConstant.NOT_FOUND, "题目不存在: " + questionId);
        }
        int submitCount = question.getSubmitCount() == null ? 0 : question.getSubmitCount();
        int acceptedCount = question.getAcceptedCount() == null ? 0 : question.getAcceptedCount();
        submitCount += 1;
        if (Boolean.TRUE.equals(accepted)) {
            acceptedCount += 1;
        }
        BigDecimal rate = BigDecimal.valueOf(submitCount == 0 ? 0 : acceptedCount * 100.0 / submitCount)
                .setScale(2, RoundingMode.HALF_UP);
        question.setSubmitCount(submitCount);
        question.setAcceptedCount(acceptedCount);
        question.setAcceptanceRate(rate);
        questionMapper.updateById(question);
        return Result.success(null);
    }

    @Override
    public Result<List<QuestionBaseDTO>> getQuestionsByChapter(Long chapterId) {
        if (chapterId == null) {
            return Result.success(List.of());
        }
        List<ExamQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getChapterId, chapterId));
        return Result.success(questions.stream().map(this::toBase).toList());
    }

    private QuestionBaseDTO toBase(ExamQuestion question) {
        QuestionBaseDTO response = new QuestionBaseDTO();
        response.setId(question.getId());
        response.setContent(question.getContent());
        response.setType(typeCode(question.getType()));
        response.setOptions(parseOptions(question.getOptions()));
        response.setRequirements(question.getRequirements());
        response.setCodeFramework(question.getCodeFramework());
        response.setChapterId(question.getChapterId());
        response.setDifficulty(difficultyCode(question.getDifficulty()));
        response.setScore(question.getScore());
        response.setJudgeMode(question.getJudgeMode());
        return response;
    }

    private QuestionDetailDTO toDetail(ExamQuestion question) {
        QuestionDetailDTO response = new QuestionDetailDTO();
        response.setId(question.getId());
        response.setContent(question.getContent());
        response.setType(typeCode(question.getType()));
        response.setOptions(parseOptions(question.getOptions()));
        response.setAnswer(question.getAnswer());
        response.setAnalysis(question.getAnalysis());
        response.setChapterId(question.getChapterId());
        response.setDifficulty(difficultyCode(question.getDifficulty()));
        response.setScore(question.getScore());
        response.setCodeFramework(question.getCodeFramework());
        response.setRequirements(question.getRequirements());
        response.setJudgeMode(question.getJudgeMode());
        response.setTestCases(toTestCaseItems(question.getTestCases()));
        return response;
    }

    private List<QuestionDetailDTO.TestCaseDTO> toTestCaseItems(List<ExamQuestion.TestCase> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            return List.of();
        }
        return testCases.stream().map(tc -> {
            QuestionDetailDTO.TestCaseDTO item = new QuestionDetailDTO.TestCaseDTO();
            item.setCaseIndex(tc.getCaseIndex());
            item.setInput(tc.getInput());
            item.setExpectedOutput(tc.getExpectedOutput());
            item.setIsSample(tc.getIsSample());
            item.setCaseType(tc.getCaseType());
            item.setTimeLimit(tc.getTimeLimit());
            item.setMemoryLimit(tc.getMemoryLimit());
            return item;
        }).toList();
    }

    private Integer typeCode(QuestionTypeEnum type) {
        return type == null ? null : type.getCode();
    }

    private Integer difficultyCode(QuestionDifficultyEnum difficulty) {
        return difficulty == null ? null : difficulty.getCode();
    }

    private List<String> parseOptions(String options) {
        if (options == null || options.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(options, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("题目选项解析失败，按单元素返回: {}", options);
            return Collections.singletonList(options);
        }
    }

}
