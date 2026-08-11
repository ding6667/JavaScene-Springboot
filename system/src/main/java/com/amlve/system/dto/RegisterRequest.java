package com.amlve.system.dto;

import com.amlve.system.constant.BusinessConstant;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class RegisterRequest {
    @NotBlank(message = BusinessConstant.REGISTER_USERNAME_EMPTY)
    private String username;
    @NotBlank(message = BusinessConstant.REGISTER_PASSWORD_EMPTY)
    private String password;
}
