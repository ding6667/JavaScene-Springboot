package com.amlve.grading.dto.request;

import com.amlve.grading.constant.ErrorMessageConstant;
import com.amlve.grading.enumeration.TaskCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GradingStartProjectRequest {
    @NotBlank(message = ErrorMessageConstant.UPLOAD_ID_EMPTY)
    private String uploadId;
    @NotNull(message = ErrorMessageConstant.STANDARD_CONFIG_ID_EMPTY)
    private Long standardConfigId;
    @NotNull(message = ErrorMessageConstant.CLASS_ID_EMPTY)
    private String classId;
    @NotNull(message = ErrorMessageConstant.TASK_CATEGORY_EMPTY)
    private TaskCategory taskCategory;
    @NotNull(message = ErrorMessageConstant.TASK_SEQUENCE_EMPTY)
    private Integer taskSequence;
}