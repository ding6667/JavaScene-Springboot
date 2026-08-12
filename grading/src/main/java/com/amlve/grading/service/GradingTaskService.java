package com.amlve.grading.service;

import com.amlve.common.request.PageResult;
import com.amlve.grading.vo.GradingTaskResponse;

public interface GradingTaskService {
    PageResult<GradingTaskResponse> pageZipTasks(Long userId, String semester, String taskCategory, Integer taskSequence,
                                                  String classId, Integer status, Integer page, Integer size, String sortOrder);
    PageResult<GradingTaskResponse> pageFileTasks(Long userId, String semester, String taskCategory, Integer taskSequence,
                                                   String studentId, Integer status, Integer page, Integer size, String sortOrder);
    GradingTaskResponse getDetail(String taskId);
}