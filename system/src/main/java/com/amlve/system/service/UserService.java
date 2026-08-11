package com.amlve.system.service;

public interface UserService {
    String checkUser(String username, String password);

    String register(String username, String password);
}
