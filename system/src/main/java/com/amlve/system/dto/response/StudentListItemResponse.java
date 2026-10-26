package com.amlve.system.dto.response;

import lombok.Data;

/**
 * 学生列表项。仅用户信息，进度概要不在本任务范围
 */
@Data
public class StudentListItemResponse {

    private Long userId;

    private String studentNo;

    private String realName;

    private Long classId;

    private String className;

    private String grade;

    private Integer enabled;
}
