package com.amlve.system.service.impl;

import com.amlve.common.security.CustomUserDetails;
import com.amlve.system.constant.BusinessConstant;
import com.amlve.system.tableEnt.User;
import com.amlve.system.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );

        if (user == null) {
            throw new UsernameNotFoundException(BusinessConstant.LOGIN_USERNAME_NOT_EXIST + username);
        }

        // enabled=0 视为禁用：登录时由 DaoAuthenticationProvider 抛 DisabledException，
        // 已签发 token 由 JwtAuthenticationFilter 拦截
        boolean enabled = user.getEnabled() != null && user.getEnabled() == 1;

        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                enabled,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRoleName()))
        );
    }
}