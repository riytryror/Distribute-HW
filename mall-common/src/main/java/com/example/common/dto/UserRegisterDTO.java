package com.example.common.dto;
import lombok.Data;

// 注册请求
@Data
public class UserRegisterDTO {
    private String username;
    private String password;
    private String phone;
}

