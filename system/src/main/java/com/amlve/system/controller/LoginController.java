package com.amlve.system.controller;

import com.amlve.common.request.Result;
import com.amlve.common.utils.JWTUtil;
import com.amlve.system.constant.BusinessConstant;
import com.amlve.system.dto.LoginDTO;
import com.amlve.system.service.UserService;
import com.amlve.system.tableEnt.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/login")
public class LoginController {

    private final UserService userService;

    /**
     * 登录
     * @param dto
     * @return
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDTO dto) {
        String token = userService.checkUser(dto.getUsername(), dto.getPassword());
        return Result.success(token);
    }
}
