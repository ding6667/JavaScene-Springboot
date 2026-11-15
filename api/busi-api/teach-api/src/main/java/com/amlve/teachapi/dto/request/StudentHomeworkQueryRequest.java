package com.amlve.teachapi.dto.request;

import lombok.Data;
import java.io.Serializable;

/**
 * 学生作业列表查询（teach 提供给 study 调用）
 */
@Data
public class StudentHomeworkQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 学生所在班级ID */
    private Long classId;

    /** 年级（classId 为空或支持全年级作业时使用） */
    private String grade;

    /** 状态过滤，默认只查已发布：2=已发布 */
    private Integer status = 2;

    /** 章节ID，可选，按章节筛作业 */
    private Long chapterId;

    /** 是否只查未截止的，可选 */
    private Boolean onlyNotExpired;

    /** 页码，从1开始 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 20;
}