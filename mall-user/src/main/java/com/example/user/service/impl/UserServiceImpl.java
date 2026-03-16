package com.example.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 修复 Wrapper
import com.baomidou.dynamic.datasource.annotation.DS;
import com.example.common.dto.UserLoginDTO;
import com.example.common.dto.UserRegisterDTO;
import com.example.common.util.JwtUtil;       
import com.example.user.entity.User;           
import com.example.user.mapper.UserMapper;
import com.example.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;               
@Service
@DS("user") // 操作 mall_user 库
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public void register(UserRegisterDTO dto) {
        // 检查用户是否存在
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (existing != null) throw new RuntimeException("用户名已存在");

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encoder.encode(dto.getPassword())); // BCrypt 加密
        user.setPhone(dto.getPhone());
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
    }

    @Override
    public String login(UserLoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        
        if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        return JwtUtil.createToken(user.getId(), user.getUsername());
    }

    @Override
    public User getById(Long id) {
        // 调用 MyBatis-Plus 提供的基础方法直接从数据库查询
        return userMapper.selectById(id);
    }
}