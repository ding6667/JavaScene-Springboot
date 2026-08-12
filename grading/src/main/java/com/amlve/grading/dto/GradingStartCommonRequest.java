package com.amlve.grading.dto;

import com.amlve.grading.constant.ErrorMessageConstant;
import com.amlve.grading.enumeration.TaskCategory;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GradingStartCommonRequest {
    @NotEmpty(message = ErrorMessageConstant.UPLOAD_IDS_EMPTY)
    private List<String> uploadIds;
    @NotNull(message = ErrorMessageConstant.STANDARD_CONFIG_ID_EMPTY)
    private Long standardConfigId;
    @NotNull(message = ErrorMessageConstant.CLASS_ID_EMPTY)
    private String classId;
    @NotNull(message = ErrorMessageConstant.TASK_CATEGORY_EMPTY)
    private TaskCategory taskCategory;
    @NotNull(message = ErrorMessageConstant.TASK_SEQUENCE_EMPTY)
    private Integer taskSequence;
}