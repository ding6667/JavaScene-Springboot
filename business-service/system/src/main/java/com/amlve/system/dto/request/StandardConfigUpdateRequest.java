package com.amlve.system.dto.request;

import com.amlve.common.entity.RubricItem;
import com.amlve.system.constant.ErrorMessageConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardConfigUpdateRequest {

    @NotBlank(message = ErrorMessageConstant.STANDARD_NAME_EMPTY)
    private String standardName;

    @NotNull(message = ErrorMessageConstant.STANDARD_CONTENT_EMPTY)
    private List<RubricItem> standardContent;

    @NotNull(message = ErrorMessageConstant.IS_CURRENT_EMPTY)
    private Integer isCurrent;
}