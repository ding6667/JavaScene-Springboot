package com.amlve.common.security;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * 自定义 UserDetails，携带数据库主键 userId，避免业务侧再从 principal 反查
 */
@Getter
public class CustomUserDetails extends User {

    /**
     * 用户ID（数据库主键）
     */
    private final Long userId;

    /**
     * @param userId      用户ID
     * @param username    用户名
     * @param password    密码（已加密）
     * @param enabled     账号是否启用，对应 sys_user.enabled；false 时 DaoAuthenticationProvider 抛 DisabledException
     * @param authorities 权限集合
     */
    public CustomUserDetails(Long userId, String username, String password, boolean enabled,
                             Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, true, true, true, authorities);
        this.userId = userId;
    }
}