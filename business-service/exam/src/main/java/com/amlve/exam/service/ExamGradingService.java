package com.amlve.exam.service;

import com.amlve.exam.dto.response.GradingResultResponse;

public interface ExamGradingService {

    /**
     * 异步批改指定学生某次作业的所有提交
     */
    void gradeAsync(Long homeworkId, Long userId);

    /**
     * 查询批改结果
     */
    GradingResultResponse getByHomeworkAndUser(Long homeworkId, Long userId);
}