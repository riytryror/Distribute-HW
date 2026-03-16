package com.example.user.service;

import com.example.common.dto.UserLoginDTO;
import com.example.common.dto.UserRegisterDTO;
import com.example.user.entity.User;

public interface UserService {
    /**
     * 用户注册
     */
    void register(UserRegisterDTO registerDTO);

    /**
     * 用户登录
     * @return JWT Token
     */
    String login(UserLoginDTO loginDTO);

    /**
     * 根据ID获取用户信息
     */
    User getById(Long userId);
}