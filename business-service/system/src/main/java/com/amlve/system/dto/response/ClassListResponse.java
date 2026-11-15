package com.amlve.system.dto.response;

import lombok.Data;

/**
 * 班级列表项
 */
@Data
public class ClassListResponse {

    private Long classId;

    private String className;

    private String grade;

    private Long courseId;

    /**
     * 课程名称，由 course 侧数据回填；课程模块未提供名称查询时为空
     */
    private String courseName;

    private Long teacherId;

    private String teacherName;

    private Integer studentCount;
}
