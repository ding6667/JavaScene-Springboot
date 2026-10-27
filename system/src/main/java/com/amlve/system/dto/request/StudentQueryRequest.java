package com.amlve.system.dto.request;

import lombok.Data;

/**
 * 学生列表查询请求
 */
@Data
public class StudentQueryRequest {

    /**
     * 班级筛选；TEACHER 不传时自动取自己带的全部班级
     */
    private Long classId;

    /**
     * 学号 / 姓名模糊关键词
     */
    private String keyword;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
