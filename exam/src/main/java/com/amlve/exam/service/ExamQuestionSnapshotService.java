package com.amlve.exam.service;

import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.exam.tableEnt.ExamQuestionSnapshot;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 题目快照服务：把一道题「定格」成可复用的快照行。
 * <p>
 * 一个题目版本只落一行，所有学生共用，错题表只存 snapshot_id 指针。
 */
public interface ExamQuestionSnapshotService {

    /**
     * 取（或创建）题目快照。
     * <p>
     * {@code teachSnapshotId} 非空时，快照内容取自 teach 发布作业时固化的那份原文
     * （题干/选项/代码框架/标准答案/测试用例），题库后续修订不影响这一版；
     * teach 不可用或快照已删时才回退到 {@code question} 的当前内容。
     *
     * @param question         题目当前内容：作业快照取不到时兜底，同时提供难度、题型等元信息
     * @param teachSnapshotId  作业题目快照ID（teach_homework_question.id）；
     *                         非空时按「作业发布快照」去重，为空时按「题库内容指纹」去重
     * @return 快照行；question 为空时返回 null
     */
    ExamQuestionSnapshot resolve(ExamQuestion question, Long teachSnapshotId);

    /**
     * 按ID取快照。
     *
     * @param snapshotId 快照ID
     * @return 快照，不存在或已删除时返回 null
     */
    ExamQuestionSnapshot getById(Long snapshotId);

    /**
     * 批量取快照，避免错题列表逐条查库。
     *
     * @param snapshotIds 快照ID集合
     * @return snapshotId → 快照
     */
    Map<Long, ExamQuestionSnapshot> mapByIds(Collection<Long> snapshotIds);

    /**
     * 取某道题的全部历史版本快照（同一题被改过多次时会有多行）。
     *
     * @param questionId 题库题目ID
     * @return 快照列表，按创建时间倒序
     */
    List<ExamQuestionSnapshot> listByQuestionId(Long questionId);
}
