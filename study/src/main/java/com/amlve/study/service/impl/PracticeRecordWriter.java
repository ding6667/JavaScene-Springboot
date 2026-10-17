package com.amlve.study.service.impl;

import com.amlve.study.mapper.PracticeMapper;
import com.amlve.study.mapper.QuestionStatusMapper;
import com.amlve.study.mapper.StudyCodeRunRecordMapper;
import com.amlve.study.tableEnt.StudyCodeRunRecord;
import com.amlve.study.tableEnt.StudyPracticeRecord;
import com.amlve.study.tableEnt.StudyQuestionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 练习记录落库：事务边界收在这里，判题（耗时）调用方放在事务外执行。
 *
 * <p>幂等口径（任务书 3.2）：practice 一题只有一条（uk_user_question），
 * run 一次运行一条；practice 与 question_status 在同一事务内更新保持一致。
 */
@Service
@RequiredArgsConstructor
public class PracticeRecordWriter {

    private final PracticeMapper practiceMapper;
    private final StudyCodeRunRecordMapper codeRunRecordMapper;
    private final QuestionStatusMapper questionStatusMapper;

    public record SavedIds(Long practiceId, Long runId) {}

    /**
     * 客观题：upsert 练习记录 + 同事务更新题目状态。
     * 调用方先按 (userId, questionId) 查出已有记录并把 id 带回，存在即更新。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long savePractice(StudyPracticeRecord record, StudyQuestionStatus questionStatus) {
        upsertPractice(record);
        upsertQuestionStatus(questionStatus);
        return record.getId();
    }

    /**
     * 编程题：先写运行记录（一次运行一条），再把主键回填给练习记录，
     * upsert 练习记录与题目状态，三条在同一事务。
     */
    @Transactional(rollbackFor = Exception.class)
    public SavedIds saveProgramming(StudyCodeRunRecord runRecord, StudyPracticeRecord practiceRecord,
                                    StudyQuestionStatus questionStatus) {
        codeRunRecordMapper.insert(runRecord);
        practiceRecord.setRunRecordId(runRecord.getId());
        upsertPractice(practiceRecord);
        upsertQuestionStatus(questionStatus);
        return new SavedIds(practiceRecord.getId(), runRecord.getId());
    }

    /** 草稿：只 upsert 练习记录，不产生运行记录、不更新题目状态 */
    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(StudyPracticeRecord record) {
        upsertPractice(record);
        return record.getId();
    }

    /** id 为空插入，否则按主键更新（uk_user_question 幂等） */
    private void upsertPractice(StudyPracticeRecord record) {
        if (record.getId() == null) {
            practiceMapper.insert(record);
        } else {
            practiceMapper.updateById(record);
        }
    }

    private void upsertQuestionStatus(StudyQuestionStatus status) {
        if (status == null) {
            return;
        }
        if (status.getId() == null) {
            questionStatusMapper.insert(status);
        } else {
            questionStatusMapper.updateById(status);
        }
    }
}
