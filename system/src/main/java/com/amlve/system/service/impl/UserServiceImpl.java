package com.amlve.system.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.common.utils.JWTUtil;
import com.amlve.system.constant.ErrorCodeConstant;
import com.amlve.system.constant.ErrorMessageConstant;
import com.amlve.system.mapper.UserMapper;
import com.amlve.system.service.UserService;
import com.amlve.system.tableEnt.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final UserMapper userMapper;
    private final JWTUtil jwtUtil;

    @Override
    public String checkUser(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getPassword, password));
        if (user == null) {
            throw new BusinessException(ErrorCodeConstant.LOGIN_ERROR, ErrorMessageConstant.LOGIN_ERROR);
        }
        return jwtUtil.generateToken(user.getId());
    }

    @Override
    public String register(String username, String password) {
        if (userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)) != null) {
            throw new BusinessException(ErrorCodeConstant.REGISTER_ERROR, ErrorMessageConstant.REGISTER_ERROR);
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        userMapper.insert(user);
        Long id = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)).getId();
        return jwtUtil.generateToken(id);
    }
}
