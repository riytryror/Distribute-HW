package com.example.common.dto;
import lombok.Data;

// 登录请求
@Data
public class UserLoginDTO {
    private String username;
    private String password;
}