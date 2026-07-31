package com.amlve.system.service;

import com.amlve.system.dto.RegisterDTO;

public interface UserService {
    String checkUser(String username, String password);

    String register(String username, String password);
}
