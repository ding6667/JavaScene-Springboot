package com.amlve.exam.service;

import com.amlve.exam.dto.request.HomeworkSubmitRequest;

/**
 * 作业提交服务。
 */
public interface ExamHomeworkService {

    /**
     * 学生提交作业答案，并在事务提交后触发异步批改。
     *
     * @param request 作业提交请求
     */
    void submit(HomeworkSubmitRequest request);
}