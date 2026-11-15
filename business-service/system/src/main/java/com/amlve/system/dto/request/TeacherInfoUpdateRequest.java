package com.amlve.system.dto.request;

import lombok.Data;

/**
 * 教师完善个人信息请求。teacherNo 首次完善必填、之后不可改
 */
@Data
public class TeacherInfoUpdateRequest {

    private String teacherNo;

    /**
     * 职称：讲师/副教授/教授
     */
    private String title;

    private String college;

    /**
     * 所属系/教研室
     */
    private String department;

    private String office;

    private String researchArea;
}
