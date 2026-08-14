package com.amlve.system.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.common.utils.JWTUtil;
import com.amlve.system.CustomUserDetails;
import com.amlve.system.constant.ErrorCodeConstant;
import com.amlve.system.constant.ErrorMessageConstant;
import com.amlve.system.mapper.UserMapper;
import com.amlve.system.service.UserService;
import com.amlve.system.tableEnt.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final UserMapper userMapper;
    private final JWTUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Override
    public String checkUser(String username, String password) {
        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
            Authentication authentication = authenticationManager.authenticate(authToken);
            CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
            return jwtUtil.generateToken(user.getUserId(), user.getUsername());
        } catch (BadCredentialsException e) {
            throw new BusinessException(ErrorCodeConstant.LOGIN_ERROR, ErrorMessageConstant.LOGIN_ERROR);
        }
    }

    @Override
    @Transactional
    public String register(String username, String password) {
        // 1. 检查用户名是否已存在
        long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count > 0) {
            throw new BusinessException(ErrorCodeConstant.REGISTER_ERROR, "用户名已存在");
        }

        // 2. 创建用户，密码使用 BCrypt 加密
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // ✅ 加密存储
        userMapper.insert(user);

        // 3. 返回 JWT（注册即登录，直接返回 Token）
        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }
}
