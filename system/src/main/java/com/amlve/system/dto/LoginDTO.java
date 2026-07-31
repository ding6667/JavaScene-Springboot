package com.amlve.system.dto;

import com.amlve.system.constant.BusinessConstant;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class LoginDTO {
    @NotBlank(message = BusinessConstant.LOGIN_USERNAME_EMPTY)
    private String username;
    @NotBlank(message = BusinessConstant.LOGIN_PASSWORD_EMPTY)
    private String password;
}
