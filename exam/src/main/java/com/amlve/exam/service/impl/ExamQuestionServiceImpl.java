package com.amlve.exam.service.impl;

import com.amlve.exam.dto.request.QuestionGenerateRequest;
import com.amlve.exam.enumeration.QuestionDifficultyEnum;
import com.amlve.exam.enumeration.QuestionTypeEnum;
import com.amlve.exam.mapper.ExamQuestionMapper;
import com.amlve.exam.service.ExamQuestionService;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 题库服务实现：提供按章节随机抽题。
 */
@Service
public class ExamQuestionServiceImpl extends ServiceImpl<ExamQuestionMapper, ExamQuestion> implements ExamQuestionService {

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
}
