package com.example.order.service;

public interface OrderService {
    /**
     * 创建秒杀订单
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 订单ID
     */
    Long createOrder(Long userId, Long productId);
}