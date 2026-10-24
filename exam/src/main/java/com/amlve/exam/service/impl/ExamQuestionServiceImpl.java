package com.amlve.exam.service.impl;

import com.amlve.common.enums.QuestionTypeEnum;
import com.amlve.common.request.PageResult;
import com.amlve.exam.dto.request.QuestionGenerateRequest;
import com.amlve.exam.enumeration.QuestionDifficultyEnum;
import com.amlve.exam.mapper.ExamQuestionMapper;
import com.amlve.exam.service.ExamQuestionService;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.examapi.dto.request.QuestionAdminQueryRequest;
import com.amlve.examapi.dto.response.QuestionAdminVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 题库服务实现：提供按章节随机抽题与管理端分页列表。
 */
@Service
public class ExamQuestionServiceImpl extends ServiceImpl<ExamQuestionMapper, ExamQuestion> implements ExamQuestionService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 按章节随机抽取题目（不调用智能体）。
     * 从符合条件的题库中随机打乱后取前 count 道；题库为空或数量不足时按实际返回。
     *
     * @param request 抽题请求
     * @return 随机抽取的题目列表（无题时为空列表）
     */
    @Override
    public List<ExamQuestion> generate(QuestionGenerateRequest request) {
        LambdaQueryWrapper<ExamQuestion> wrapper = Wrappers.<ExamQuestion>lambdaQuery()
                .eq(ExamQuestion::getChapterId, request.getChapterId())
                .eq(request.getType() != null, ExamQuestion::getType, QuestionTypeEnum.fromCode(request.getType()))
                .eq(request.getDifficulty() != null, ExamQuestion::getDifficulty, QuestionDifficultyEnum.fromCode(request.getDifficulty()));
        List<ExamQuestion> all = list(wrapper);
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        Collections.shuffle(all);
        int want = request.getCount() == null ? all.size() : request.getCount();
        int count = Math.min(want, all.size());
        return new ArrayList<>(all.subList(0, count));
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
                .eq(request.getDifficulty() != null, ExamQuestion::getDifficulty, QuestionDifficultyEnum.fromCode(request.getDifficulty()))
                .like(StringUtils.hasText(request.getKeyword()), ExamQuestion::getContent, request.getKeyword())
                .orderByDesc(ExamQuestion::getCreateTime);

        Page<ExamQuestion> pageData = page(new Page<>(pageNum, pageSize), wrapper);

        PageResult<QuestionAdminVO> result = new PageResult<>();
        result.setRecords(pageData.getRecords().stream().map(this::toAdminVO).toList());
        result.setTotal(pageData.getTotal());
        result.setCurrent(pageData.getCurrent());
        result.setSize(pageData.getSize());
        return result;
    }

    /**
     * 题目实体转管理端视图（题型/难度枚举转回数字编码，选项 JSON 转列表）。
     */
    private QuestionAdminVO toAdminVO(ExamQuestion question) {
        QuestionAdminVO vo = new QuestionAdminVO();
        vo.setId(question.getId());
        vo.setContent(question.getContent());
        if (question.getType() != null) {
            vo.setType(question.getType().getCode());
            vo.setTypeDesc(question.getType().getDesc());
        }
        vo.setOptions(parseOptions(question.getOptions()));
        vo.setAnswer(question.getAnswer());
        vo.setAnalysis(question.getAnalysis());
        vo.setChapterId(question.getChapterId());
        if (question.getDifficulty() != null) {
            vo.setDifficulty(question.getDifficulty().getCode());
        }
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
        vo.setSubmitCount(question.getSubmitCount());
        vo.setAcceptedCount(question.getAcceptedCount());
        vo.setAcceptanceRate(question.getAcceptanceRate());
        vo.setCreateTime(question.getCreateTime());
        vo.setUpdateTime(question.getUpdateTime());
        return vo;
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