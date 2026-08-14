package com.amlve.common.configuration;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * 自定义 Authentication，直接携带 userId
 */
public class UserIdAuthenticationToken extends AbstractAuthenticationToken {

    @Getter
    private final Long userId;
    @Getter
    private final String username;
    private final UserDetails userDetails; // 可选，如果需要权限信息

    public UserIdAuthenticationToken(Long userId, String username, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        this.username = username;
        this.userDetails = null;
        setAuthenticated(true); // 表示已认证
    }

    // 如果需要权限，可传入完整的 UserDetails
    public UserIdAuthenticationToken(UserDetails userDetails, Long userId, String username) {
        super(userDetails.getAuthorities());
        this.userId = userId;
        this.username = username;
        this.userDetails = userDetails;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // 不需要凭证
    }

    @Override
    public Object getPrincipal() {
        // 返回包含用户信息的东西，这里我们返回 username，也可以返回自定义对象
        return this.username;
    }

}