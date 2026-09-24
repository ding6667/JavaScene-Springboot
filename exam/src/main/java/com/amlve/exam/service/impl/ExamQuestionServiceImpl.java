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

@Service
public class ExamQuestionServiceImpl extends ServiceImpl<ExamQuestionMapper, ExamQuestion> implements ExamQuestionService {

    @Override
    public List<ExamQuestion> generate(QuestionGenerateRequest request) {
        LambdaQueryWrapper<ExamQuestion> wrapper = Wrappers.<ExamQuestion>lambdaQuery()
                .eq(ExamQuestion::getChapterId, request.getChapterId())
                .eq(request.getType() != null, ExamQuestion::getType, QuestionTypeEnum.fromCode(request.getType()))
                .eq(request.getDifficulty() != null, ExamQuestion::getDifficulty, QuestionDifficultyEnum.fromCode(request.getDifficulty()));
        List<ExamQuestion> all = list(wrapper);
        if (all.isEmpty()) {
            return List.of();
        }
        Collections.shuffle(all);
        int count = Math.min(request.getCount(), all.size());
        return new ArrayList<>(all.subList(0, count));
    }
}