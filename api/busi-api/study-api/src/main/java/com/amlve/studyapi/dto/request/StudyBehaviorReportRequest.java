package com.amlve.studyapi.dto.request;

import com.amlve.studyapi.constant.BehaviorType;
import lombok.Data;

import java.util.Map;

/**
 * 学习行为上报请求（供 study / qa / exam 模块调用）
 *
 * <p>上报后异步落库到 study_behavior 表，失败不影响调用方主流程。
 */
@Data
public class StudyBehaviorReportRequest {

    /**
     * 学生ID
     */
    private Long userId;

    /**
     * 行为类型
     */
    private BehaviorType behaviorType;

    /**
     * 关联章节ID（可为空）
     */
    private Long chapterId;

    /**
     * 关联题目ID（可为空）
     */
    private Long questionId;

    /**
     * 行为耗时（秒，可为空）
     */
    private Integer duration;

    /**
     * 行为详情（自由扩展，JSON 落库）
     */
    private Map<String, Object> detail;
}
