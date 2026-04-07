package com.example.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SeckillOrderMessage implements Serializable {
    private Long orderId;
    private Long userId;
    private Long productId;
    private Integer count;
    private Long requestTime;
}
