package com.amlve.examapi.dto.request;

import lombok.Data;

/**
 * 管理端题目列表查询请求。
 * <p>
 * 供 /api/exam/question/admin/list 使用，仅教师/管理员可见。
 */
@Data
public class QuestionAdminQueryRequest {

    /**
     * 章节ID（可选）
     */
    private Long chapterId;

    /**
     * 题型：1=选择，2=填空，3=判断，4=程序阅读，5=编程，6=简答（可选）
     */
    private Integer type;

    /**
     * 难度：1=基础，2=进阶，3=拔高（可选）
     */
    private Integer difficulty;

    /**
     * 题干关键字（可选，模糊匹配）
     */
    private String keyword;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}