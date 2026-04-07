package com.example.order.service;

import com.example.common.dto.SeckillOrderMessage;
import com.example.common.dto.SeckillResultDTO;
import com.example.order.entity.Order;

import java.util.List;

public interface OrderService {
    /**
     * 创建秒杀订单
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 订单ID
     */
    Long createOrder(Long userId, Long productId);

    Long submitSeckill(Long userId, Long productId);

    void processSeckillOrder(SeckillOrderMessage message);

    SeckillResultDTO querySeckillResult(Long userId, Long productId);

    Order getById(Long orderId);

    List<Order> listByUserId(Long userId);
}