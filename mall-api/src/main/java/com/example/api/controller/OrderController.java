package com.example.api.controller;

import com.example.common.dto.SeckillResultDTO;
import com.example.common.result.Result;
import com.example.order.entity.Order;
import com.example.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/seckill/{productId}")
    public Result<Long> seckill(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = Long.valueOf(String.valueOf(request.getAttribute("userId")));
        Long orderId = orderService.submitSeckill(userId, productId);
        return Result.success(orderId);
    }

    @GetMapping("/seckill/result")
    public Result<SeckillResultDTO> seckillResult(@RequestParam Long productId, HttpServletRequest request) {
        Long userId = Long.valueOf(String.valueOf(request.getAttribute("userId")));
        return Result.success(orderService.querySeckillResult(userId, productId));
    }

    @GetMapping("/orders/{orderId}")
    public Result<Order> getById(@PathVariable Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        return Result.success(order);
    }

    @GetMapping("/orders/me")
    public Result<List<Order>> myOrders(HttpServletRequest request) {
        Long userId = Long.valueOf(String.valueOf(request.getAttribute("userId")));
        return Result.success(orderService.listByUserId(userId));
    }
}
