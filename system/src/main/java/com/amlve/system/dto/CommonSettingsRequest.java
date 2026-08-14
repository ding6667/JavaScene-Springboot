package com.amlve.system.dto;

import com.amlve.system.constant.ErrorMessageConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class CommonSettingsRequest {
    @NotNull(message = ErrorMessageConstant.CONFIG_KEY_EMPTY)
    private Integer configKey;
    @NotBlank(message = ErrorMessageConstant.CONFIG_VALUE_EMPTY)
    private String configValue;
}
