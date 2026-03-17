package com.example.api.controller;

import com.example.common.result.Result;
import com.example.common.dto.UserLoginDTO;
import com.example.common.dto.UserRegisterDTO;
import com.example.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success(null);
    }

    @Value("${server.port}")
    private String port;

    @PostMapping("/login")
    public Result<String> login(@RequestBody UserLoginDTO dto) {
        //// 打印当前处理请求的实例端口，方便在控制台验证负载均衡
        System.out.println("收到登录请求，当前处理实例的端口为: " + port);
        
        String token = userService.login(dto);
        return Result.success(token);
    }
}