package com.amlve.exam.service.impl;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.exam.enumeration.QuestionDifficultyEnum;
import com.amlve.common.enums.QuestionTypeEnum;
import com.amlve.exam.mapper.ExamQuestionMapper;
import com.amlve.exam.service.ExamQuestionPracticeService;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.examapi.dto.request.PracticeQueryRequest;
import com.amlve.examapi.dto.response.PracticeDetailVO;
import com.amlve.examapi.dto.response.PracticeItemVO;
import com.amlve.studyapi.dto.response.PracticeStatusResponse;
import com.amlve.studyapi.service.StudyPracticeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 学生刷题服务实现。
 * <p>
 * 公共统计（通过率/提交次数）来自 exam_question；个人状态（done/correct/attemptCount/lastAnswer）
 * 通过 study-api 获取，study 侧实现缺失时置空兜底，不阻断主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamQuestionPracticeServiceImpl implements ExamQuestionPracticeService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ExamQuestionMapper questionMapper;
    private final ObjectProvider<StudyPracticeService> studyPracticeServiceProvider;

    @Override
    public PageResult<PracticeItemVO> listPractice(Long userId, PracticeQueryRequest request) {
        int pageNum = request.getPageNum() == null ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();

        LambdaQueryWrapper<ExamQuestion> wrapper = Wrappers.<ExamQuestion>lambdaQuery()
                .eq(request.getChapterId() != null, ExamQuestion::getChapterId, request.getChapterId())
                .eq(request.getType() != null, ExamQuestion::getType, QuestionTypeEnum.fromCode(request.getType()))
                .eq(request.getDifficulty() != null, ExamQuestion::getDifficulty, QuestionDifficultyEnum.fromCode(request.getDifficulty()))
                .orderByDesc(ExamQuestion::getId);
        Page<ExamQuestion> pageData = questionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<ExamQuestion> records = pageData.getRecords();

        Map<Long, PracticeStatusResponse> statusMap = loadStatusBatch(userId, records);

        List<PracticeItemVO> items = new ArrayList<>();
        for (ExamQuestion question : records) {
            PracticeStatusResponse status = statusMap.get(question.getId());
            if (Boolean.TRUE.equals(request.getOnlyUndone())
                    && status != null && Boolean.TRUE.equals(status.getDone())) {
                continue;
            }
            items.add(toItemVO(question, status));
        }

        PageResult<PracticeItemVO> result = new PageResult<>();
        result.setRecords(items);
        result.setTotal(pageData.getTotal());
        result.setCurrent(pageData.getCurrent());
        result.setSize(pageData.getSize());
        return result;
    }

    @Override
    public PracticeDetailVO getPracticeDetail(Long questionId) {
        ExamQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCodeConstant.NOT_FOUND, "题目不存在: " + questionId);
        }
        PracticeDetailVO vo = new PracticeDetailVO();
        vo.setId(question.getId());
        vo.setContent(question.getContent());
        vo.setType(typeCode(question.getType()));
        vo.setOptions(parseOptions(question.getOptions()));
        vo.setChapterId(question.getChapterId());
        vo.setDifficulty(difficultyCode(question.getDifficulty()));
        vo.setScore(question.getScore());
        vo.setCodeFramework(question.getCodeFramework());
        vo.setRequirements(question.getRequirements());
        vo.setSampleTestCases(toSampleCases(question.getTestCases()));
        vo.setAcceptanceRate(toDouble(question.getAcceptanceRate()));

        PracticeStatusResponse status = loadStatus(questionId);
        if (status != null) {
            vo.setDone(status.getDone());
            vo.setLastAnswer(status.getLastAnswer());
        }
        return vo;
    }

    private PracticeItemVO toItemVO(ExamQuestion question, PracticeStatusResponse status) {
        PracticeItemVO vo = new PracticeItemVO();
        vo.setId(question.getId());
        vo.setContent(question.getContent());
        vo.setType(typeCode(question.getType()));
        vo.setOptions(parseOptions(question.getOptions()));
        vo.setChapterId(question.getChapterId());
        vo.setDifficulty(difficultyCode(question.getDifficulty()));
        vo.setScore(question.getScore());
        vo.setCodeFramework(question.getCodeFramework());
        vo.setRequirements(question.getRequirements());
        vo.setAcceptanceRate(toDouble(question.getAcceptanceRate()));
        vo.setSubmitCount(question.getSubmitCount());
        if (status != null) {
            vo.setDone(status.getDone());
            vo.setCorrect(status.getCorrect());
            vo.setAttemptCount(status.getAttemptCount());
        }
        // favorited 暂无数据源，置空占位
        vo.setFavorited(null);
        return vo;
    }

    private Map<Long, PracticeStatusResponse> loadStatusBatch(Long userId, List<ExamQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return Map.of();
        }
        StudyPracticeService service = studyPracticeServiceProvider.getIfAvailable();
        if (service == null) {
            log.warn("StudyPracticeService 实现不存在，刷题个人状态置空");
            return Map.of();
        }
        List<Long> questionIds = questions.stream().map(ExamQuestion::getId).toList();
        try {
            Result<Map<Long, PracticeStatusResponse>> result = service.getQuestionStatusBatch(userId, questionIds);
            return result == null || result.getData() == null ? Map.of() : result.getData();
        } catch (Exception e) {
            log.warn("批量查询刷题个人状态失败: {}", e.getMessage());
            return Map.of();
        }
    }

    private PracticeStatusResponse loadStatus(Long questionId) {
        StudyPracticeService service = studyPracticeServiceProvider.getIfAvailable();
        if (service == null) {
            return null;
        }
        try {
            Result<PracticeStatusResponse> result = service.getQuestionStatus(SecurityUtils.getUserId(), questionId);
            return result == null ? null : result.getData();
        } catch (Exception e) {
            log.warn("查询单题状态失败: {}", e.getMessage());
            return null;
        }
    }

    private List<PracticeDetailVO.TestCaseVO> toSampleCases(List<ExamQuestion.TestCase> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            return List.of();
        }
        List<PracticeDetailVO.TestCaseVO> result = new ArrayList<>();
        for (ExamQuestion.TestCase tc : testCases) {
            if (!Boolean.TRUE.equals(tc.getIsSample())) {
                continue;
            }
            PracticeDetailVO.TestCaseVO vo = new PracticeDetailVO.TestCaseVO();
            vo.setInput(tc.getInput());
            vo.setExpectedOutput(tc.getExpectedOutput());
            vo.setDescription(tc.getDescription());
            result.add(vo);
        }
        return result;
    }

    private Integer typeCode(QuestionTypeEnum type) {
        return type == null ? null : type.getCode();
    }

    private Integer difficultyCode(QuestionDifficultyEnum difficulty) {
        return difficulty == null ? null : difficulty.getCode();
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
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
