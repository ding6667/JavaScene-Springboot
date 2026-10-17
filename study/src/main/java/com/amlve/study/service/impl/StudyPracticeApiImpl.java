package com.amlve.study.service.impl;

import com.amlve.common.request.Result;
import com.amlve.study.mapper.PracticeMapper;
import com.amlve.study.mapper.QuestionStatusMapper;
import com.amlve.study.tableEnt.StudyPracticeRecord;
import com.amlve.study.tableEnt.StudyQuestionStatus;
import com.amlve.studyapi.dto.response.PracticeStatusDTO;
import com.amlve.studyapi.service.StudyPracticeApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 刷题状态 API 实现（供 exam 调用，任务书 3.8）。
 *
 * <p>口径（任务书 5.4 定稿）：done / correct / attemptCount 只从
 * study_question_status 表取，lastAnswer 取 study_practice_record 最新答案；
 * favorited 收藏功能暂未开放，固定 false。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyPracticeApiImpl implements StudyPracticeApi {

    /** 题目状态：2=已通过 */
    private static final int STATUS_PASSED = 2;
    /** 题目状态：1=尝试过 */
    private static final int STATUS_ATTEMPTED = 1;

    private final QuestionStatusMapper questionStatusMapper;
    private final PracticeMapper practiceMapper;

    @Override
    public Result<PracticeStatusDTO> getQuestionStatus(Long userId, Long questionId) {
        if (userId == null || questionId == null) {
            return Result.error("userId / questionId 不能为空");
        }
        StudyQuestionStatus status = selectStatus(userId, questionId);
        StudyPracticeRecord practice = selectPractice(userId, questionId);
        return Result.success(toDTO(questionId, status, practice));
    }

    @Override
    public Result<Map<Long, PracticeStatusDTO>> getQuestionStatusBatch(Long userId, List<Long> questionIds) {
        if (userId == null || questionIds == null || questionIds.isEmpty()) {
            return Result.success(Map.of());
        }
        List<Long> distinctIds = questionIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Result.success(Map.of());
        }

        Map<Long, StudyQuestionStatus> statusMap = questionStatusMapper.selectList(
                        new LambdaQueryWrapper<StudyQuestionStatus>()
                                .eq(StudyQuestionStatus::getUserId, userId)
                                .in(StudyQuestionStatus::getQuestionId, distinctIds))
                .stream()
                .collect(Collectors.toMap(StudyQuestionStatus::getQuestionId, Function.identity(), (a, b) -> a));

        Map<Long, StudyPracticeRecord> practiceMap = practiceMapper.selectList(
                        new LambdaQueryWrapper<StudyPracticeRecord>()
                                .eq(StudyPracticeRecord::getUserId, userId)
                                .in(StudyPracticeRecord::getQuestionId, distinctIds))
                .stream()
                .collect(Collectors.toMap(StudyPracticeRecord::getQuestionId, Function.identity(), (a, b) -> a));

        Map<Long, PracticeStatusDTO> result = new LinkedHashMap<>();
        for (Long questionId : distinctIds) {
            result.put(questionId, toDTO(questionId, statusMap.get(questionId), practiceMap.get(questionId)));
        }
        return Result.success(result);
    }

    private StudyQuestionStatus selectStatus(Long userId, Long questionId) {
        return questionStatusMapper.selectOne(new LambdaQueryWrapper<StudyQuestionStatus>()
                .eq(StudyQuestionStatus::getUserId, userId)
                .eq(StudyQuestionStatus::getQuestionId, questionId)
                .last("LIMIT 1"));
    }

    private StudyPracticeRecord selectPractice(Long userId, Long questionId) {
        return practiceMapper.selectOne(new LambdaQueryWrapper<StudyPracticeRecord>()
                .eq(StudyPracticeRecord::getUserId, userId)
                .eq(StudyPracticeRecord::getQuestionId, questionId)
                .last("LIMIT 1"));
    }

    private PracticeStatusDTO toDTO(Long questionId, StudyQuestionStatus status, StudyPracticeRecord practice) {
        PracticeStatusDTO dto = new PracticeStatusDTO();
        dto.setQuestionId(questionId);
        int statusCode = status == null || status.getStatus() == null ? 0 : status.getStatus();
        dto.setDone(statusCode >= STATUS_ATTEMPTED);
        dto.setCorrect(statusCode == STATUS_PASSED);
        dto.setAttemptCount(status == null || status.getAttemptCount() == null ? 0 : status.getAttemptCount());
        dto.setLastAnswer(practice == null ? null : practice.getAnswerContent());
        // 收藏功能暂未开放
        dto.setFavorited(false);
        return dto;
    }
}
