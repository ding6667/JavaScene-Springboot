package com.amlve.exam.service.impl;

import com.amlve.agentquestionapi.dto.request.QuestionGenerateRequest;
import com.amlve.agentquestionapi.dto.response.QuestionGenerateResponse;
import com.amlve.agentquestionapi.service.QuestionAgent;
import com.amlve.common.exception.BusinessException;
import com.amlve.exam.dto.request.QuestionAiGenerateRequest;
import com.amlve.exam.enumeration.QuestionDifficultyEnum;
import com.amlve.common.enums.QuestionTypeEnum;
import com.amlve.exam.mapper.ExamQuestionDifficultyConfigMapper;
import com.amlve.exam.mapper.ExamQuestionMapper;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.exam.tableEnt.ExamQuestionDifficultyConfig;
import com.amlve.knowledgeapi.dto.response.WeightConfigVO;
import com.amlve.knowledgeapi.service.KnowledgeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 出题编排（exam 侧）。
 * 职责：根据难度配置ID查表 → 计算知识点分配 allocation → 组装出题请求 → 调用出题智能体（Dify）→ 结果落库 exam_question。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExamQuestionGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** 判题方式：1=AI批改 */
    private static final int JUDGE_MODE_AI = 1;
    /** 判题方式：2=测试用例判题 */
    private static final int JUDGE_MODE_TEST_CASE = 2;

    private final KnowledgeService knowledgeService;
    private final QuestionAgent questionAgent;
    private final ExamQuestionMapper questionMapper;
    private final ExamQuestionDifficultyConfigMapper difficultyConfigMapper;

    public int generate(QuestionAiGenerateRequest request) {
        // 1. 查难度配置
        ExamQuestionDifficultyConfig config = difficultyConfigMapper.selectById(request.getDifficultyConfigId());
        if (config == null) {
            throw new BusinessException("难度配置不存在: id=" + request.getDifficultyConfigId());
        }

        // 2. 计算知识点分配
        List<WeightConfigVO> weightConfigs = knowledgeService.getWeightConfigsByChapter(request.getChapterId()).getData();
        List<QuestionGenerateRequest.AllocationItem> allocation = computeAllocation(weightConfigs, request.getCount());
        if (allocation.isEmpty()) {
            throw new BusinessException("该章节暂无知识点配置，无法出题");
        }

        // 3. 组装出题智能体请求
        QuestionGenerateRequest agentRequest = buildAgentRequest(request, allocation, config);

        // 4. 调用出题智能体（Dify）
        QuestionGenerateResponse response = questionAgent.generate(agentRequest).getData();
        if (response == null || "failed".equals(response.getStatus())) {
            log.error("出题失败: {}", response == null ? "响应为空" : response.getErrorMsg());
            return 0;
        }

        // 5. 落库
        int saved = saveGeneratedQuestions(request.getChapterId(), response, allocation);
        return saved > 0 ? 1 : 0;
    }

    private List<QuestionGenerateRequest.AllocationItem> computeAllocation(List<WeightConfigVO> configs, int count) {
        List<QuestionGenerateRequest.AllocationItem> items = new ArrayList<>();
        if (configs == null || configs.isEmpty()) {
            return items;
        }
        if (configs.size() >= count) {
            for (int i = 0; i < count; i++) {
                items.add(buildItem(configs.get(i), 1));
            }
            return items;
        }
        int totalWeight = 0;
        for (WeightConfigVO c : configs) {
            totalWeight += weightOf(c);
        }
        int remaining = count;
        for (int i = 0; i < configs.size(); i++) {
            WeightConfigVO c = configs.get(i);
            int allocated = (i == configs.size() - 1)
                    ? remaining
                    : Math.max(1, (int) Math.round((double) count * weightOf(c) / totalWeight));
            items.add(buildItem(c, allocated));
            remaining -= allocated;
        }
        return items;
    }

    private int weightOf(WeightConfigVO config) {
        return config.getWeight() == null ? 1 : Math.max(1, config.getWeight().intValue());
    }

    private QuestionGenerateRequest.AllocationItem buildItem(WeightConfigVO config, int count) {
        QuestionGenerateRequest.AllocationItem item = new QuestionGenerateRequest.AllocationItem();
        item.setPointId(String.valueOf(config.getKnowledgePointId()));
        item.setName(config.getKnowledgePointName());
        item.setCount(String.valueOf(count));
        return item;
    }

    private QuestionGenerateRequest buildAgentRequest(QuestionAiGenerateRequest request,
                                                       List<QuestionGenerateRequest.AllocationItem> allocation,
                                                       ExamQuestionDifficultyConfig config) {
        QuestionGenerateRequest.DifficultyConfig difficulty = new QuestionGenerateRequest.DifficultyConfig();
        difficulty.setCode(config.getCode());
        difficulty.setName(config.getName());
        difficulty.setDescription(config.getDescription());
        difficulty.setExample(config.getExample());
        difficulty.setCognitiveLevels(config.getCognitiveLevels());

        QuestionGenerateRequest agentRequest = new QuestionGenerateRequest();
        agentRequest.setChapterId(request.getChapterId());
        agentRequest.setChapterName(request.getChapterName());
        agentRequest.setDifficulty(difficulty);
        agentRequest.setQuestionType(request.getQuestionType());
        agentRequest.setCount(request.getCount());
        agentRequest.setUserInstruction(request.getUserInstruction());
        agentRequest.setAllocation(allocation);
        return agentRequest;
    }

    private int saveGeneratedQuestions(Long chapterId,
                                       QuestionGenerateResponse response,
                                       List<QuestionGenerateRequest.AllocationItem> allocation) {
        if (response == null || response.getQuestions() == null) {
            log.warn("出题智能体未返回题目: chapterId={}", chapterId);
            return 0;
        }
        Map<String, Long> knowledgePointIds = buildKnowledgePointIdMap(allocation);
        int saved = 0;
        for (QuestionGenerateResponse.QuestionItem item : response.getQuestions()) {
            QuestionTypeEnum type = mapType(item.getType());
            if (type == null) {
                log.warn("无法映射题型到 QuestionTypeEnum，跳过落库: type={}, knowledgePoint={}",
                        item.getType(), item.getKnowledgePoint());
                continue;
            }
            ExamQuestion question = new ExamQuestion();
            question.setChapterId(chapterId);
            question.setContent(item.getContent());
            question.setType(type);
            question.setOptions(toOptionsJson(item.getOptions()));
            question.setRequirements(item.getRequirements());
            boolean programming = type == QuestionTypeEnum.PROGRAMMING;
            // 非编程题的代码框架留空（验收 9.1）
            question.setCodeFramework(programming ? item.getCodeFramework() : null);
            question.setHints(item.getHints());
            question.setRelatedQuestionIds(toIdList(item.getRelatedQuestions()));
            question.setAnswer(item.getAnswer());
            question.setAnalysis(item.getAnalysis());
            question.setSolutionOutline(item.getSolutionOutline());
            question.setDifficulty(mapDifficulty(item.getDifficulty()));
            question.setDifficultyScore(item.getDifficultyScore());
            question.setCognitiveLevel(item.getCognitiveLevel());
            question.setAbilityDimensions(item.getAbilityDimensions());
            question.setCommonMistakes(item.getCommonMistakes());
            question.setKnowledgePointIds(resolveKnowledgePointIds(item, knowledgePointIds));
            question.setWeight(item.getWeight());
            question.setScore(5);
            question.setJudgeMode(resolveJudgeMode(type, item));
            question.setTestCases(toTestCases(item.getTestCases()));
            if (questionMapper.insert(question) == 0) {
                log.error("生成题目落库失败，跳过: content={}", item.getContent());
                continue;
            }
            saved++;
        }
        return saved;
    }

    private Map<String, Long> buildKnowledgePointIdMap(List<QuestionGenerateRequest.AllocationItem> allocation) {
        Map<String, Long> map = new HashMap<>();
        if (allocation == null) {
            return map;
        }
        for (QuestionGenerateRequest.AllocationItem item : allocation) {
            if (item.getName() == null || item.getPointId() == null) {
                continue;
            }
            try {
                map.put(item.getName(), Long.valueOf(item.getPointId()));
            } catch (NumberFormatException e) {
                log.warn("知识点ID非数字，跳过映射: pointId={}", item.getPointId());
            }
        }
        return map;
    }

    private List<Long> resolveKnowledgePointIds(QuestionGenerateResponse.QuestionItem item, Map<String, Long> nameToId) {
        List<String> names = item.getKnowledgePoints();
        if (names == null || names.isEmpty()) {
            if (item.getKnowledgePoint() == null) {
                return List.of();
            }
            names = List.of(item.getKnowledgePoint());
        }
        List<Long> ids = new ArrayList<>();
        for (String name : names) {
            Long id = nameToId.get(name);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private List<Long> toIdList(List<String> values) {
        List<Long> ids = new ArrayList<>();
        if (values == null) {
            return ids;
        }
        for (String value : values) {
            try {
                ids.add(Long.valueOf(value));
            } catch (NumberFormatException e) {
                log.debug("相似题标识非数字，跳过: {}", value);
            }
        }
        return ids;
    }

    private Integer resolveJudgeMode(QuestionTypeEnum type, QuestionGenerateResponse.QuestionItem item) {
        boolean hasTestCases = item.getTestCases() != null && !item.getTestCases().isEmpty();
        if (type == QuestionTypeEnum.PROGRAMMING && hasTestCases) {
            return JUDGE_MODE_TEST_CASE;
        }
        return JUDGE_MODE_AI;
    }

    private List<ExamQuestion.TestCase> toTestCases(List<QuestionGenerateResponse.TestCase> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            return List.of();
        }
        List<ExamQuestion.TestCase> result = new ArrayList<>();
        for (QuestionGenerateResponse.TestCase tc : testCases) {
            ExamQuestion.TestCase entity = new ExamQuestion.TestCase();
            entity.setCaseIndex(tc.getCaseIndex());
            entity.setInput(tc.getInput());
            entity.setExpectedOutput(tc.getExpectedOutput());
            entity.setIsSample(tc.getIsSample());
            entity.setCaseType(tc.getCaseType());
            entity.setWeight(tc.getWeight());
            entity.setTimeLimit(tc.getTimeLimit());
            entity.setMemoryLimit(tc.getMemoryLimit());
            entity.setDescription(tc.getDescription());
            result.add(entity);
        }
        return result;
    }

    private QuestionTypeEnum mapType(String type) {
        if (type == null) {
            return null;
        }
        return switch (type.trim().toLowerCase()) {
            case "choice", "select", "single_choice", "multiple_choice" -> QuestionTypeEnum.CHOICE;
            case "fill", "fill_in", "fill_blank" -> QuestionTypeEnum.FILL_IN;
            case "judge", "true_false" -> QuestionTypeEnum.JUDGE;
            case "read", "program_read" -> QuestionTypeEnum.PROGRAM_READ;
            case "programming", "code" -> QuestionTypeEnum.PROGRAMMING;
            case "short_answer", "short" -> QuestionTypeEnum.SHORT_ANSWER;
            default -> null;
        };
    }

    private QuestionDifficultyEnum mapDifficulty(String difficulty) {
        QuestionDifficultyEnum result = QuestionDifficultyEnum.fromDifficulty(difficulty);
        return result == null ? QuestionDifficultyEnum.BASIC : result;
    }

    private String toOptionsJson(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(options);
        } catch (Exception e) {
            log.warn("序列化选项失败: {}", e.getMessage());
            return String.join(";", options);
        }
    }
}
