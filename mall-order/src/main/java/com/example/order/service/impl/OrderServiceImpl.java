package com.example.order.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.example.order.entity.Order;
import com.example.order.mapper.OrderMapper;
import com.example.order.service.OrderService;
import com.example.stock.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@DS("order") // 关键：指定此服务操作 mall_order 数据库
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private StockService stockService; // 注入库存模块的服务

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务
    public Long createOrder(Long userId, Long productId) {
        log.info("开始创建订单: 用户ID={}, 商品ID={}", userId, productId);

        // 1. 调用库存服务扣减库存 (跨库调用)
        // stockService 内部使用了 @DS("stock")，会自动切换到 mall_stock 库
        // 如果库存不足，该方法会抛出 RuntimeException
        stockService.deduct(productId, 1);

        // 2. 扣减成功，创建订单记录
        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(productId);
        order.setCount(1);
        order.setStatus(0); // 设置为待支付状态
        order.setCreateTime(LocalDateTime.now());

        orderMapper.insert(order);

        log.info("订单创建成功: 订单ID={}", order.getId());
        return order.getId();
    }
}