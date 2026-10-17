package com.amlve.studyapi.service;

import com.amlve.common.request.Result;
import com.amlve.studyapi.dto.request.StudyBehaviorReportRequest;

/**
 * 学习行为上报 API（study 模块实现，qa / exam 等模块可调用）
 *
 * <p>行为数据异步落库（study_behavior 表），为学情分析积累数据；
 * 上报失败不影响调用方主流程。
 */
public interface StudyBehaviorApi {

    /**
     * 上报一条学习行为（异步落库）
     */
    Result<Void> report(StudyBehaviorReportRequest request);
}
