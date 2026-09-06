package com.amlve.studyapi.dto.response;

public class ClassStatisticsResponse {
    /**
     * 班级总人数
     */
    private Integer totalStudents;

    /**
     * 班级平均学习进度（0-100）
     */
    private Double avgProgress;

    /**
     * 班级名称（可选，便于前端展示）
     */
    private String className;

    /**
     * 已开始学习人数
     */
    private Integer startedCount;

    /**
     * 已完成全部章节人数
     */
    private Integer completedCount;
}
