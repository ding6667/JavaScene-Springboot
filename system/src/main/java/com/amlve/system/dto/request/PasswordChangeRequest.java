package com.amlve.system.dto.request;

import com.amlve.system.constant.ErrorMessageConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 本人修改密码请求，需校验旧密码
 */
@Data
public class PasswordChangeRequest {

    @NotBlank(message = ErrorMessageConstant.OLD_PASSWORD_EMPTY)
    private String oldPassword;

    @NotBlank(message = ErrorMessageConstant.NEW_PASSWORD_EMPTY)
    @Size(min = 6, max = 32, message = ErrorMessageConstant.USER_PASSWORD_SIZE)
    private String newPassword;
}
