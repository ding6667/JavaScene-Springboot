package com.amlve.studyapi.service;

import com.amlve.common.request.Result;
import com.amlve.studyapi.dto.response.ClassStatisticsResponse;

public interface StudyStatisticsService {
    /**
     * 获取班级学习统计数据（总人数 + 平均进度）
     *
     * @param classId 班级ID
     * @return 班级统计数据
     */
    Result<ClassStatisticsResponse> getClassStatistics(Long classId);
}
