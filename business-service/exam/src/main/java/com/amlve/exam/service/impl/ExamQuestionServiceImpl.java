package com.amlve.exam.service.impl;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.enums.QuestionTypeEnum;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.exam.dto.request.QuestionGenerateRequest;
import com.amlve.exam.mapper.ExamQuestionMapper;
import com.amlve.exam.service.ExamDifficultyConfigService;
import com.amlve.exam.service.ExamQuestionService;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.exam.util.QuestionRenderConfigCodec;
import com.amlve.examapi.dto.request.QuestionAdminQueryRequest;
import com.amlve.examapi.dto.response.QuestionAdminVO;
import com.amlve.examapi.dto.response.QuestionTestCaseVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 题库服务实现：提供按题型+难度配比随机抽题与管理端分页列表。
 */
@Slf4j
@Service
public class ExamQuestionServiceImpl extends ServiceImpl<ExamQuestionMapper, ExamQuestion> implements ExamQuestionService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private ExamDifficultyConfigService difficultyConfigService;

    /**
     * 按题型 + 难度配比随机抽题（不调用智能体）。
     * <p>
     * 每个题型内可再按难度档分配，例如"选择题 3 道 = 简单 1 道 + 拔高 2 道"；每档独立随机取，
     * 某档题量不足时按实际返回并记日志，不影响其他档。未指定 difficultyAllocations 时按题型整体抽 count 道。
     *
     * @param request 抽题请求
     * @return 题型 → 题目视图列表（按难度档顺序拼接；无题时为空列表）
     */
    @Override
    public Map<Integer, List<QuestionAdminVO>> generate(QuestionGenerateRequest request) {
        Map<Integer, List<ExamQuestion>> selectedEqs = new LinkedHashMap<>();
        for (QuestionGenerateRequest.TypeAllocation typeAllocation : request.getTypeAllocations()) {
            List<ExamQuestion> picked = new ArrayList<>();
            List<QuestionGenerateRequest.DifficultyAllocation> allocations = typeAllocation.getDifficultyAllocations();
            if (allocations == null || allocations.isEmpty()) {
                picked.addAll(pickByDifficulty(request.getChapterId(), typeAllocation.getType(),
                        null, typeAllocation.getCount()));
            } else {
                for (QuestionGenerateRequest.DifficultyAllocation allocation : allocations) {
                    requireDifficultyExists(allocation.getDifficultyConfigId());
                    picked.addAll(pickByDifficulty(request.getChapterId(), typeAllocation.getType(),
                            allocation.getDifficultyConfigId(), allocation.getCount()));
                }
            }
            selectedEqs.put(typeAllocation.getType(), picked);
        }
        // 抽出来的题统一转管理端视图：answer 以结构化对象下发，难度名称一次性批量取
        Map<Long, String> difficultyNames = difficultyConfigService.namesByIds(
                selectedEqs.values().stream().flatMap(List::stream)
                        .map(ExamQuestion::getDifficultyConfigId).toList());
        Map<Integer, List<QuestionAdminVO>> result = new LinkedHashMap<>();
        selectedEqs.forEach((type, questions) -> result.put(type, questions.stream()
                .map(question -> toAdminVO(question, difficultyNames)).toList()));
        return result;
    }

    /**
     * 单个（题型,难度）档位随机抽 count 道。
     *
     * @param difficultyConfigId 难度配置ID，null 表示不限难度
     * @param count              需要题数，null 表示该档全取
     */
    private List<ExamQuestion> pickByDifficulty(Long chapterId, Integer type, Long difficultyConfigId, Integer count) {
        List<ExamQuestion> all = list(Wrappers.<ExamQuestion>lambdaQuery()
                .eq(ExamQuestion::getChapterId, chapterId)
                .eq(ExamQuestion::getType, QuestionTypeEnum.fromCode(type))
                .eq(difficultyConfigId != null, ExamQuestion::getDifficultyConfigId, difficultyConfigId));
        if (all.isEmpty()) {
            log.warn("题库无符合条件的题目: chapterId={}, type={}, difficultyConfigId={}", chapterId, type, difficultyConfigId);
            return List.of();
        }
        Collections.shuffle(all);
        int want = count == null ? all.size() : count;
        int take = Math.min(want, all.size());
        if (take < want) {
            log.warn("题库题量不足，按实际返回: chapterId={}, type={}, difficultyConfigId={}, 需要={}, 实取={}",
                    chapterId, type, difficultyConfigId, want, take);
        }
        return new ArrayList<>(all.subList(0, take));
    }

    /** 难度配置必须先存在，否则该档永远抽不到题、前端只会看到一个空列表 */
    private void requireDifficultyExists(Long difficultyConfigId) {
        if (difficultyConfigId == null) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "难度配比必须指定难度配置ID");
        }
        if (difficultyConfigService.nameOf(difficultyConfigId) == null) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "难度配置不存在：id=" + difficultyConfigId);
        }
    }

    /**
     * 管理端题目分页列表：按章节/题型/难度/关键字过滤，返回含答案的完整视图。
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<QuestionAdminVO> listForAdmin(QuestionAdminQueryRequest request) {
        int pageNum = request.getPageNum() == null ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();

        LambdaQueryWrapper<ExamQuestion> wrapper = Wrappers.<ExamQuestion>lambdaQuery()
                .eq(request.getChapterId() != null, ExamQuestion::getChapterId, request.getChapterId())
                .eq(request.getType() != null, ExamQuestion::getType, QuestionTypeEnum.fromCode(request.getType()))
                .eq(request.getDifficultyConfigId() != null, ExamQuestion::getDifficultyConfigId, request.getDifficultyConfigId())
                .like(StringUtils.hasText(request.getKeyword()), ExamQuestion::getContent, request.getKeyword())
                .orderByDesc(ExamQuestion::getCreateTime);

        Page<ExamQuestion> pageData = page(new Page<>(pageNum, pageSize), wrapper);
        // 难度名称一次性批量取，避免逐题查配置表
        Map<Long, String> difficultyNames = difficultyConfigService.namesByIds(
                pageData.getRecords().stream().map(ExamQuestion::getDifficultyConfigId).toList());

        PageResult<QuestionAdminVO> result = new PageResult<>();
        result.setRecords(pageData.getRecords().stream()
                .map(question -> toAdminVO(question, difficultyNames)).toList());
        result.setTotal(pageData.getTotal());
        result.setCurrent(pageData.getCurrent());
        result.setSize(pageData.getSize());
        return result;
    }

    /**
     * 管理端查单题：与列表同一套视图，answer 回结构化对象，并带上编程题测试用例与相似题，
     * 让修改页面拿到什么就能原样提交什么。
     *
     * @param id 题目ID
     * @return 题目视图
     */
    @Override
    public QuestionAdminVO getForAdmin(Long id) {
        ExamQuestion question = getById(id);
        if (question == null) {
            throw new BusinessException(ErrorMessageConstant.QUESTION_NOT_FOUND);
        }
        Map<Long, String> difficultyNames = difficultyConfigService.namesByIds(
                Collections.singleton(question.getDifficultyConfigId()));
        return toAdminVO(question, difficultyNames);
    }

    /**
     * 题目实体转管理端视图（题型转数字编码、难度联配置表取名，选项 JSON 转列表）。
     */
    private QuestionAdminVO toAdminVO(ExamQuestion question, Map<Long, String> difficultyNames) {
        QuestionAdminVO vo = new QuestionAdminVO();
        vo.setId(question.getId());
        vo.setContent(question.getContent());
        vo.setShortTitle(question.getShortTitle());
        vo.setRenderConfig(QuestionRenderConfigCodec.forRender(question.getRenderConfig(),
                question.getType(), question.getContent(), question.getAnswer()));
        if (question.getType() != null) {
            vo.setType(question.getType().getCode());
            vo.setTypeDesc(question.getType().getDesc());
        }
        vo.setOptions(parseOptions(question.getOptions()));
        vo.setAnswer(question.getAnswer());
        vo.setAnalysis(question.getAnalysis());
        vo.setChapterId(question.getChapterId());
        vo.setDifficultyConfigId(question.getDifficultyConfigId());
        vo.setDifficultyName(difficultyNames.get(question.getDifficultyConfigId()));
        vo.setDifficultyScore(question.getDifficultyScore());
        vo.setCognitiveLevel(question.getCognitiveLevel());
        vo.setScore(question.getScore());
        vo.setWeight(question.getWeight());
        vo.setJudgeMode(question.getJudgeMode());
        vo.setRequirements(question.getRequirements());
        vo.setCodeFramework(question.getCodeFramework());
        vo.setHints(question.getHints());
        vo.setSolutionOutline(question.getSolutionOutline());
        vo.setAbilityDimensions(question.getAbilityDimensions());
        vo.setCommonMistakes(question.getCommonMistakes());
        vo.setKnowledgePointIds(question.getKnowledgePointIds());
        vo.setTestCases(toTestCaseVOs(question.getTestCases()));
        vo.setRelatedQuestionIds(question.getRelatedQuestionIds());
        vo.setSubmitCount(question.getSubmitCount());
        vo.setAcceptedCount(question.getAcceptedCount());
        vo.setAcceptanceRate(question.getAcceptanceRate());
        vo.setCreateTime(question.getCreateTime());
        vo.setUpdateTime(question.getUpdateTime());
        return vo;
    }

    /**
     * 编程题测试用例转视图（含 weight / description，供修改页面原样回填）。
     */
    private List<QuestionTestCaseVO> toTestCaseVOs(List<ExamQuestion.TestCase> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            return List.of();
        }
        return testCases.stream().map(tc -> {
            QuestionTestCaseVO vo = new QuestionTestCaseVO();
            vo.setCaseIndex(tc.getCaseIndex());
            vo.setInput(tc.getInput());
            vo.setExpectedOutput(tc.getExpectedOutput());
            vo.setIsSample(tc.getIsSample());
            vo.setCaseType(tc.getCaseType());
            vo.setWeight(tc.getWeight());
            vo.setTimeLimit(tc.getTimeLimit());
            vo.setMemoryLimit(tc.getMemoryLimit());
            vo.setDescription(tc.getDescription());
            return vo;
        }).toList();
    }

    /**
     * 选项 JSON 字符串转列表；空值返回空列表，非法 JSON 降级为单元素列表。
     */
    private List<String> parseOptions(String optionsJson) {
        if (!StringUtils.hasText(optionsJson)) {
            return List.of();
        }
        try {
            List<String> options = OBJECT_MAPPER.readValue(optionsJson, new TypeReference<List<String>>() {
            });
            return options == null ? List.of() : options;
        } catch (Exception e) {
            return List.of(optionsJson);
        }
    }
}