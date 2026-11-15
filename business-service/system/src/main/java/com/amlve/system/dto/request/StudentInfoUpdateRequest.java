package com.amlve.system.dto.request;

import lombok.Data;

/**
 * 学生完善个人信息请求。
 * <p>
 * studentNo 首次完善必填、之后不可改；classId 由管理员分配，学生侧只读；
 * graduationYear 不在自填范围（由管理员维护或按 enrollYear 推算）
 */
@Data
public class StudentInfoUpdateRequest {

    private String studentNo;

    private String major;

    private String college;

    private Integer enrollYear;
}
