package com.amlve.exam.service.impl;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.exam.service.ExamWrongQuestionService;
import com.amlve.examapi.dto.request.WrongQuestionArchiveRequest;
import com.amlve.examapi.dto.response.WrongQuestionRetryDTO;
import com.amlve.examapi.service.ExamWrongQuestionApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 错题本跨模块接口实现（供 study 判分后上报错题、重做判题取载荷）。
 * <p>
 * 真正的落库逻辑在 {@link ExamWrongQuestionService}，这里只做参数校验、
 * 批次去重与事务边界，避免 study 侧重复实现错题本规则。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamWrongQuestionApiImpl implements ExamWrongQuestionApi {

    /** 来源缺省值：1=作业 */
    private static final int SOURCE_DEFAULT = 1;

    private final ExamWrongQuestionService wrongQuestionService;

    @Override
    public Result<Void> archiveWrongQuestion(Long userId, WrongQuestionArchiveRequest request) {
        if (userId == null || request == null || request.getQuestionId() == null) {
            log.warn("错题归档参数不完整: userId={}, questionId={}", userId,
                    request == null ? null : request.getQuestionId());
            return Result.error(ErrorCodeConstant.PARAM_INVALID, "学生ID与题目ID不能为空");
        }
        wrongQuestionService.archive(userId, withDefaultSource(request));
        return Result.success(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Integer> archiveWrongQuestions(Long userId, List<WrongQuestionArchiveRequest> requests) {
        if (userId == null) {
            log.warn("批量错题归档缺少学生ID");
            return Result.error(ErrorCodeConstant.PARAM_INVALID, "学生ID不能为空");
        }
        if (requests == null || requests.isEmpty()) {
            return Result.success(0);
        }
        // 同一批内按「题目 + 快照 + 来源」去重：同一题错两次只累计一次，避免 wrongCount 被重复 +1；
        // 但同一题的不同版本必须分开，不能按 questionId 一刀切。
        Map<String, WrongQuestionArchiveRequest> merged = new LinkedHashMap<>();
        for (WrongQuestionArchiveRequest request : requests) {
            if (request == null || request.getQuestionId() == null) {
                continue;
            }
            merged.put(dedupKey(request), withDefaultSource(request));
        }
        for (WrongQuestionArchiveRequest request : merged.values()) {
            wrongQuestionService.archive(userId, request);
        }
        log.debug("批量错题归档: userId={}, 入参{}条, 实际归档{}条", userId, requests.size(), merged.size());
        return Result.success(merged.size());
    }

    @Override
    public Result<WrongQuestionRetryDTO> getRetryPayload(Long userId, Long wrongQuestionId) {
        if (userId == null || wrongQuestionId == null) {
            return Result.error(ErrorCodeConstant.PARAM_INVALID, "学生ID与错题ID不能为空");
        }
        try {
            return Result.success(wrongQuestionService.getRetryPayload(userId, wrongQuestionId));
        } catch (BusinessException e) {
            log.warn("取错题重做载荷失败: userId={}, wrongQuestionId={}, message={}",
                    userId, wrongQuestionId, e.getMessage());
            int code = e.getCode() == null ? ErrorCodeConstant.BUSINESS_ERROR : e.getCode();
            return Result.error(code, e.getMessage());
        }
    }

    private WrongQuestionArchiveRequest withDefaultSource(WrongQuestionArchiveRequest request) {
        if (request.getSource() == null) {
            request.setSource(SOURCE_DEFAULT);
        }
        return request;
    }

    private String dedupKey(WrongQuestionArchiveRequest request) {
        return request.getQuestionId()
                + "#" + Objects.toString(request.getSnapshotId(), "-")
                + "#" + Objects.toString(request.getHomeworkSnapshotId(), "-")
                + "#" + Objects.toString(request.getSource(), "-");
    }
}
