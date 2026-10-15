package com.amlve.exam.service.impl;

import com.amlve.agentquestionapi.dto.request.QuestionGenerateRequest;
import com.amlve.agentquestionapi.dto.response.QuestionGenerateResponse;
import com.amlve.agentquestionapi.service.QuestionAgentClient;
import com.amlve.common.exception.BusinessException;
import com.amlve.exam.dto.request.QuestionAiGenerateRequest;
import com.amlve.exam.enumeration.QuestionDifficultyEnum;
import com.amlve.exam.enumeration.QuestionTypeEnum;
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
import java.util.List;

/**
 * 出题智能体（exam 侧编排实现）。
 * 职责：根据难度配置ID查表 → 计算知识点分配 allocation → 组装出题请求 → 调用出题智能体（Dify）→ 结果落库 exam_question。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QuestionAgentImpl {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final KnowledgeService knowledgeService;
    private final QuestionAgentClient questionAgentClient;
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
        QuestionGenerateResponse response = questionAgentClient.generate(agentRequest).getData();

        // 5. 落库
        int saved = saveGeneratedQuestions(request.getChapterId(), response);
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

    private int saveGeneratedQuestions(Long chapterId, QuestionGenerateResponse response) {
        if (response == null || response.getQuestions() == null) {
            log.warn("出题智能体未返回题目: chapterId={}", chapterId);
            return 0;
        }
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
            question.setAnswer(buildAnswer(item.getAnswer(), item.getAnalysis()));
            question.setDifficulty(mapDifficulty(item.getDifficulty()));
            question.setScore(5);
            if (questionMapper.insert(question) == 0) {
                log.error("生成题目落库失败，跳过: content={}", item.getContent());
                continue;
            }
            saved++;
        }
        return saved;
    }

    private QuestionTypeEnum mapType(String type) {
        return QuestionTypeEnum.fromType(type);
    }

    private QuestionDifficultyEnum mapDifficulty(String difficulty) {
        QuestionDifficultyEnum result = QuestionDifficultyEnum.fromDifficulty(difficulty);
        return result == null ? QuestionDifficultyEnum.BASIC : result;
    }

    private ExamQuestion.QuestionAnswer buildAnswer(String answer, String analysis) {
        ExamQuestion.QuestionAnswer qa = new ExamQuestion.QuestionAnswer();
        qa.setAnswer(answer);
        qa.setAnlysis(analysis);
        return qa;
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
