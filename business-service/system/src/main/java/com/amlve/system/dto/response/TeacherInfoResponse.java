package com.amlve.system.dto.response;

import lombok.Data;

/**
 * 教师扩展信息响应
 */
@Data
public class TeacherInfoResponse {

    private Long userId;

    private String teacherNo;

    private String title;

    private String college;

    private String department;

    private String office;

    private String researchArea;
}
