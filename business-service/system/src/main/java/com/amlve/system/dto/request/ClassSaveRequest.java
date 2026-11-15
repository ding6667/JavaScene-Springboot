package com.amlve.system.dto.request;

import com.amlve.system.constant.ErrorMessageConstant;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 班级新增 / 更新请求。
 * <p>
 * 不含 id：新增走 POST，更新走 PUT /api/class/{id}，被操作对象只在 URL 里出现一次，
 * 不存在「路径 id 与 body id 不一致」这类 bug
 */
@Data
public class ClassSaveRequest {

    @NotBlank(message = ErrorMessageConstant.CLASS_NAME_EMPTY)
    private String className;

    /**
     * 任课教师用户ID
     */
    private Long teacherId;

    /**
     * 年级，如 "2024级"
     */
    private String grade;

    /**
     * 所属课程ID
     */
    private Long courseId;
}
