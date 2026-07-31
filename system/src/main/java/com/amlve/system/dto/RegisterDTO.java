package com.amlve.system.dto;

import com.amlve.system.constant.BusinessConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class RegisterDTO {
    @NotBlank(message = BusinessConstant.REGISTER_USERNAME_EMPTY)
    private String username;
    @NotBlank(message = BusinessConstant.REGISTER_PASSWORD_EMPTY)
    private String password;
}
