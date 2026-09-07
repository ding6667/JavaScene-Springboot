package com.amlve.teach.dto;

import lombok.Data;

/**
 * 班级学情统计返回给前端的数据
 */
@Data
public class ClassStatsResponse {

    /** 班级学生总人数 */
    private Long totalStudents;

    /** 班级平均学习进度（百分比，如 65.5 表示 65.5%） */
    private Double averageProgress;

    /** 提示信息（study模块未接入时说明数据来源） */
    private String message;
}
