package com.amlve.system.dto.response;

import lombok.Data;

/**
 * 学生扩展信息响应
 */
@Data
public class StudentInfoResponse {

    private Long userId;

    private String studentNo;

    private Long classId;

    private String className;

    private String grade;

    private String major;

    private String college;

    private Integer enrollYear;

    private Integer graduationYear;
}
