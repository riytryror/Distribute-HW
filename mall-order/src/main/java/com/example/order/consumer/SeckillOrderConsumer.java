package com.example.order.consumer;

import com.example.common.dto.SeckillOrderMessage;
import com.example.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SeckillOrderConsumer {

    @Autowired
    private OrderService orderService;

    @KafkaListener(topics = "${seckill.kafka.topic-order-create:seckill-order-create}")
    public void onMessage(SeckillOrderMessage message) {
        log.info("收到秒杀下单消息: orderId={}, userId={}, productId={}",
                message.getOrderId(), message.getUserId(), message.getProductId());
        orderService.processSeckillOrder(message);
    }
}
