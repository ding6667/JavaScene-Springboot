package com.amlve.exam.service;

import com.amlve.exam.dto.request.HomeworkSubmitRequest;

public interface ExamHomeworkService {

    /**
     * 学生提交作业答案，并触发异步批改
     */
    void submit(HomeworkSubmitRequest request);
}