package com.amlve.examapi.service;

import com.amlve.common.request.Result;
import com.amlve.examapi.dto.request.WrongQuestionArchiveRequest;
import com.amlve.examapi.dto.response.WrongQuestionRetryDTO;

import java.util.List;

/**
 * 错题本 API（供其他模块调用）。
 * <p>
 * 学生错题本由 exam 模块维护：题库题目内容、题目快照、难度都在 exam，
 * study 判分后只负责上报「谁错了哪道题、错的是什么答案」。
 */
public interface ExamWrongQuestionApi {

    /**
     * 归档一道错题：不存在则新增，存在则错误次数 +1 并刷新最近答案（同时重新置为未掌握）。
     *
     * @param userId  学生ID
     * @param request 归档请求（题目ID、错误答案、来源、作业快照ID）
     * @return 成功/参数错误
     */
    Result<Void> archiveWrongQuestion(Long userId, WrongQuestionArchiveRequest request);

    /**
     * 批量归档错题（作业批阅、一次提交多题时用）。
     * <p>
     * 同一次调用里重复的「题目 + 来源」只记一次（保留最后一条的 lastAnswer）；
     * 非法项（questionId 为空）跳过不报错。
     *
     * @param userId   学生ID
     * @param requests 归档请求列表，可为空
     * @return 实际归档的题目数
     */
    Result<Integer> archiveWrongQuestions(Long userId, List<WrongQuestionArchiveRequest> requests);

    /**
     * 取错题重做载荷（只给服务端判题用，含标准答案与测试用例）。
     * <p>
     * 重做必须按快照判题：学生重做的是当时做错的那一版题，
     * 按题库最新版判会出现「按原来那版写对了却判错」。
     *
     * @param userId          学生ID
     * @param wrongQuestionId 错题记录ID
     * @return 重做载荷；错题不存在或不属于该学生时返回错误码
     */
    Result<WrongQuestionRetryDTO> getRetryPayload(Long userId, Long wrongQuestionId);
}
