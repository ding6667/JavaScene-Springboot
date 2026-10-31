package com.amlve.exam.service;

import com.amlve.exam.dto.response.WrongQuestionRetryVO;
import com.amlve.examapi.dto.request.WrongQuestionArchiveRequest;
import com.amlve.examapi.dto.response.WrongQuestionRetryDTO;
import com.amlve.examapi.dto.response.WrongQuestionVO;

import java.util.List;

/**
 * 错题归档服务。
 * <p>
 * 存储模型两张表：题目快照（exam_question_snapshot，一个题目版本一行、全站复用）
 * 与错题记录（exam_wrong_question，学生 × 快照 × 来源）。
 * 因此错题展示不读题库，题库改题不会让学生看到「题变了、答案对不上」。
 */
public interface ExamWrongQuestionService {

    /**
     * 归档一道错题：不存在则新增，存在则累计错误次数并更新最近答案（重新置为未掌握）。
     * <p>
     * 题目内容由 exam 侧按快照生成，调用方只负责说明「谁错了哪道题、错的什么」。
     *
     * @param userId  学生ID
     * @param request 归档请求（题目ID、错误答案、来源、作业快照ID / 指定快照ID）
     */
    void archive(Long userId, WrongQuestionArchiveRequest request);

    /**
     * 查询学生错题本，题面取自错题记录指向的快照。
     *
     * @param userId 学生ID
     * @return 错题列表
     */
    List<WrongQuestionVO> listWrongQuestions(Long userId);

    /**
     * 错题重做视图（学生端）：只含可下发的题目内容。
     *
     * @param userId          学生ID
     * @param wrongQuestionId 错题记录ID
     * @return 重做视图；记录不存在或不属于该学生时抛业务异常
     */
    WrongQuestionRetryVO getRetryView(Long userId, Long wrongQuestionId);

    /**
     * 错题重做载荷（含判题包，跨模块给 study 判题用，禁止下发学生端）。
     *
     * @param userId          学生ID
     * @param wrongQuestionId 错题记录ID
     * @return 重做载荷
     */
    WrongQuestionRetryDTO getRetryPayload(Long userId, Long wrongQuestionId);

    /**
     * 标记单条错题已掌握（同一题可能有多个版本，按记录ID定位）。
     *
     * @param userId          学生ID
     * @param wrongQuestionId 错题记录ID
     */
    void markMastered(Long userId, Long wrongQuestionId);

    /**
     * 标记某道题的全部版本错题已掌握（兼容按题目ID操作的调用方）。
     *
     * @param userId     学生ID
     * @param questionId 题库题目ID
     * @return 实际更新条数
     */
    int markMasteredByQuestion(Long userId, Long questionId);
}
