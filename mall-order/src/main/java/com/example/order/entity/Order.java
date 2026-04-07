package com.example.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_order") // 对应 mall_order 库
public class Order {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long userId;
    private Long productId;
    private Integer count;
    private Integer status; // 0: 待支付, 1: 已支付, 2: 已取消
    private LocalDateTime createTime;
}