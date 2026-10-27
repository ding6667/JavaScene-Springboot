package com.amlve.exam.service.impl;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.exam.mapper.ExamQuestionMapper;
import com.amlve.exam.mapper.ExamWrongQuestionMapper;
import com.amlve.exam.service.ExamWrongQuestionService;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.exam.tableEnt.ExamWrongQuestion;
import com.amlve.examapi.dto.response.WrongQuestionVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 错题归档服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamWrongQuestionServiceImpl implements ExamWrongQuestionService {

    private static final int MASTERED = 1;
    private static final int NOT_MASTERED = 0;
    private static final int SOURCE_HOMEWORK = 1;

    private final ExamWrongQuestionMapper wrongQuestionMapper;
    private final ExamQuestionMapper questionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long userId, Long questionId, String lastAnswer, Integer source) {
        if (userId == null || questionId == null) {
            return;
        }
        ExamWrongQuestion exist = wrongQuestionMapper.selectOne(Wrappers.<ExamWrongQuestion>lambdaQuery()
                .eq(ExamWrongQuestion::getUserId, userId)
                .eq(ExamWrongQuestion::getQuestionId, questionId));
        LocalDateTime now = LocalDateTime.now();
        if (exist == null) {
            ExamWrongQuestion record = new ExamWrongQuestion();
            record.setUserId(userId);
            record.setQuestionId(questionId);
            record.setSource(source == null ? SOURCE_HOMEWORK : source);
            record.setWrongCount(1);
            record.setLastAnswer(lastAnswer);
            record.setMastered(NOT_MASTERED);
            record.setLastWrongTime(now);
            wrongQuestionMapper.insert(record);
            return;
        }
        exist.setWrongCount((exist.getWrongCount() == null ? 0 : exist.getWrongCount()) + 1);
        exist.setLastAnswer(lastAnswer);
        exist.setMastered(NOT_MASTERED);
        exist.setLastWrongTime(now);
        if (source != null) {
            exist.setSource(source);
        }
        wrongQuestionMapper.updateById(exist);
    }

    @Override
    public List<WrongQuestionVO> listWrongQuestions(Long userId) {
        List<ExamWrongQuestion> records = wrongQuestionMapper.selectList(Wrappers.<ExamWrongQuestion>lambdaQuery()
                .eq(ExamWrongQuestion::getUserId, userId)
                .orderByDesc(ExamWrongQuestion::getLastWrongTime));
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        List<Long> questionIds = records.stream().map(ExamWrongQuestion::getQuestionId).distinct().toList();
        List<ExamQuestion> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, ExamQuestion> questionMap = (questions == null ? List.<ExamQuestion>of() : questions).stream()
                .collect(Collectors.toMap(ExamQuestion::getId, Function.identity()));

        List<WrongQuestionVO> result = new ArrayList<>();
        for (ExamWrongQuestion record : records) {
            WrongQuestionVO vo = new WrongQuestionVO();
            vo.setId(record.getId());
            vo.setQuestionId(record.getQuestionId());
            vo.setWrongCount(record.getWrongCount());
            vo.setLastAnswer(record.getLastAnswer());
            vo.setMastered(record.getMastered());
            vo.setLastWrongTime(record.getLastWrongTime());
            ExamQuestion question = questionMap.get(record.getQuestionId());
            if (question != null) {
                vo.setContent(question.getContent());
                vo.setType(question.getType() == null ? null : question.getType().getCode());
                vo.setDifficulty(question.getDifficulty() == null ? null : question.getDifficulty().getCode());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public void markMastered(Long userId, Long questionId) {
        ExamWrongQuestion record = wrongQuestionMapper.selectOne(Wrappers.<ExamWrongQuestion>lambdaQuery()
                .eq(ExamWrongQuestion::getUserId, userId)
                .eq(ExamWrongQuestion::getQuestionId, questionId));
        if (record == null) {
            throw new BusinessException(ErrorMessageConstant.WRONG_QUESTION_NOT_FOUND);
        }
        record.setMastered(MASTERED);
        wrongQuestionMapper.updateById(record);
    }
}
