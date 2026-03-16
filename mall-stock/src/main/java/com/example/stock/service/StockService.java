package com.example.stock.service;

public interface StockService {
    /**
     * 扣减库存
     * @param productId 商品ID
     * @param count 扣减数量
     */
    void deduct(Long productId, Integer count);

    /**
     * 回滚库存
     */
    void rollback(Long productId, Integer count);

    /**
     * 查询当前库存余额
     */
    Integer getAvailableStock(Long productId);
}