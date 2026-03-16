package com.example.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_user") // 对应 mall_user 库中的 t_user 表
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password; // 存储 BCrypt 密文
    private String phone;
    private LocalDateTime createTime;
}