package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_stock") // 对应 mall_stock 逻辑库
public class Stock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;      // 商品ID
    private Integer availableStock; // 可用库存（秒杀直接扣减这个字段）
    private Integer frozenStock;    // 锁定库存（下单未支付时使用，此处可预留）
}