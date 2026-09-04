package com.amlve.system.dto.request;

import com.amlve.system.constant.ErrorMessageConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassRequest {

    private Long id;

    @NotBlank(message = ErrorMessageConstant.CLASS_NAME_EMPTY)
    private String className;

    @NotBlank(message = ErrorMessageConstant.CLASS_CODE_EMPTY)
    private String classCode;

    @NotNull(message = ErrorMessageConstant.GRADE_EMPTY)
    private Integer grade;

    @NotNull(message = ErrorMessageConstant.USER_ID_EMPTY)
    private Long userId;

    private String semester;

    private Integer status;
}