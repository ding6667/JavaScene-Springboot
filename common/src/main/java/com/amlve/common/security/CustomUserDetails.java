package com.amlve.common.security;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * 自定义 UserDetails，携带 userId 等扩展信息
 */
@Getter
public class CustomUserDetails extends User {

    /**
     * 用户ID（数据库主键）
     */
    private final Long userId;

    /**
     * 用户真实姓名（可选，方便业务使用）
     */
    private final String realName;

    /**
     * 邮箱（可选）
     */
    private final String email;

    /**
     * 构造方法
     *
     * @param userId      用户ID
     * @param username    用户名
     * @param password    密码（已加密）
     * @param realName    真实姓名
     * @param email       邮箱
     * @param authorities 权限集合
     */
    public CustomUserDetails(Long userId, String username, String password,
                             String realName, String email,
                             Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.userId = userId;
        this.realName = realName;
        this.email = email;
    }

    /**
     * 简化构造方法（只传必要字段，权限默认 ROLE_USER）
     */
    public CustomUserDetails(Long userId, String username, String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.userId = userId;
        this.realName = null;
        this.email = null;
    }
}