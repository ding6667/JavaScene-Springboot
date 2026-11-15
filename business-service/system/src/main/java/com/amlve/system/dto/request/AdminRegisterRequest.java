package com.amlve.system.dto.request;

import com.amlve.system.constant.BusinessConstant;
import com.amlve.system.constant.ErrorMessageConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员注册请求（系统后台专用，需初始化密钥）
 */
@Data
public class AdminRegisterRequest {

    /**
     * 登录账号（唯一）
     */
    @NotBlank(message = BusinessConstant.REGISTER_USERNAME_EMPTY)
    private String username;

    /**
     * 密码（明文传输，服务端 BCrypt 加密存储）
     */
    @NotBlank(message = BusinessConstant.REGISTER_PASSWORD_EMPTY)
    @Size(min = 6, max = 32, message = ErrorMessageConstant.USER_PASSWORD_SIZE)
    private String password;

    /**
     * 管理员初始化密钥（与应用配置 app.admin.init-key 比对，防止公开接口被恶意注册管理员）
     */
    @NotBlank(message = ErrorMessageConstant.ADMIN_INIT_KEY_EMPTY)
    private String adminKey;
}
