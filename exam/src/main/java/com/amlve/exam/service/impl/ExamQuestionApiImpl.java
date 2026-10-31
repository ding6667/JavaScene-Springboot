package com.amlve.exam.service.impl;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.request.Result;
import com.amlve.common.enums.QuestionTypeEnum;
import com.amlve.exam.mapper.ExamQuestionMapper;
import com.amlve.exam.service.ExamDifficultyConfigService;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.exam.util.QuestionAnswerCodec;
import com.amlve.exam.util.QuestionRenderConfigCodec;
import com.amlve.examapi.dto.response.QuestionBaseDTO;
import com.amlve.examapi.dto.response.QuestionDetailResponse;
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
import java.util.Map;

/**
 * 题库跨模块 API 实现：供 study / teach 查询题目详情、批量基本信息与回写统计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamQuestionApiImpl implements ExamQuestionApi {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ExamQuestionMapper questionMapper;

    private final ExamDifficultyConfigService difficultyConfigService;

    @Override
    public Result<QuestionDetailResponse> getQuestionDetail(Long questionId) {
        ExamQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            log.warn("QUESTION_NOT_FOUND 定位: {}", questionId);
            return Result.error(ErrorCodeConstant.NOT_FOUND, ErrorMessageConstant.QUESTION_NOT_FOUND);
        }
        return Result.success(toDetail(question));
    }

    @Override
    public Result<List<QuestionBaseDTO>> getQuestionsByIds(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Result.success(List.of());
        }
        List<ExamQuestion> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, String> difficultyNames = difficultyConfigService.namesByIds(
                questions.stream().map(ExamQuestion::getDifficultyConfigId).toList());
        return Result.success(questions.stream().map(question -> toBase(question, difficultyNames)).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateStatistics(Long questionId, Boolean accepted) {
        ExamQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            log.warn("QUESTION_NOT_FOUND 定位: {}", questionId);
            return Result.error(ErrorCodeConstant.NOT_FOUND, ErrorMessageConstant.QUESTION_NOT_FOUND);
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
        Map<Long, String> difficultyNames = difficultyConfigService.namesByIds(
                questions.stream().map(ExamQuestion::getDifficultyConfigId).toList());
        return Result.success(questions.stream().map(question -> toBase(question, difficultyNames)).toList());
    }

    private QuestionBaseDTO toBase(ExamQuestion question, Map<Long, String> difficultyNames) {
        QuestionBaseDTO response = new QuestionBaseDTO();
        response.setId(question.getId());
        response.setContent(question.getContent());
        response.setShortTitle(question.getShortTitle());
        response.setRenderConfig(QuestionRenderConfigCodec.forRender(question.getRenderConfig(),
                question.getType(), question.getContent(), question.getAnswer()));
        response.setType(typeCode(question.getType()));
        response.setOptions(parseOptions(question.getOptions()));
        response.setRequirements(question.getRequirements());
        response.setCodeFramework(question.getCodeFramework());
        response.setChapterId(question.getChapterId());
        response.setDifficultyConfigId(question.getDifficultyConfigId());
        response.setDifficultyName(difficultyNames.get(question.getDifficultyConfigId()));
        response.setScore(question.getScore());
        response.setJudgeMode(question.getJudgeMode());
        return response;
    }

    private QuestionDetailResponse toDetail(ExamQuestion question) {
        QuestionDetailResponse response = new QuestionDetailResponse();
        response.setId(question.getId());
        response.setContent(question.getContent());
        response.setShortTitle(question.getShortTitle());
        // 渲染配置按题型补齐后再给出：发布作业时快照直接拿这个值，历史题也不会缺 blankCount/choiceMode
        response.setRenderConfig(QuestionRenderConfigCodec.forRender(question.getRenderConfig(),
                question.getType(), question.getContent(), question.getAnswer()));
        response.setType(typeCode(question.getType()));
        response.setOptions(parseOptions(question.getOptions()));
        response.setAnswer(QuestionAnswerCodec.toText(question.getAnswer()));
        response.setAnalysis(question.getAnalysis());
        response.setChapterId(question.getChapterId());
        response.setDifficultyConfigId(question.getDifficultyConfigId());
        response.setDifficultyName(difficultyConfigService.nameOf(question.getDifficultyConfigId()));
        response.setScore(question.getScore());
        response.setCodeFramework(question.getCodeFramework());
        response.setKnowledgePointIds(question.getKnowledgePointIds());
        response.setRequirements(question.getRequirements());
        response.setHints(question.getHints());
        response.setSolutionOutline(question.getSolutionOutline());
        response.setJudgeMode(question.getJudgeMode());
        response.setTestCases(toTestCaseItems(question.getTestCases()));
        return response;
    }

    private List<QuestionDetailResponse.TestCaseItem> toTestCaseItems(List<ExamQuestion.TestCase> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            return List.of();
        }
        return testCases.stream().map(tc -> {
            QuestionDetailResponse.TestCaseItem item = new QuestionDetailResponse.TestCaseItem();
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