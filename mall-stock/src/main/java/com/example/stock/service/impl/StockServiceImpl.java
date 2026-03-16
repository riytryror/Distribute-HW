package com.example.stock.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.stock.entity.Stock;
import com.example.stock.mapper.StockMapper;
import com.example.stock.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@DS("stock") // 关键：指定此服务操作 mall_stock 数据库
public class StockServiceImpl implements StockService {

    @Autowired
    private StockMapper stockMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(Long productId, Integer count) {
        log.info("开始扣减库存: 商品ID={}, 数量={}", productId, count);

        // 执行 SQL 扣减
        int affectedRows = stockMapper.decreaseStock(productId, count);

        // 如果受影响行数为 0，说明可用库存小于扣减数量，即库存不足
        if (affectedRows == 0) {
            log.warn("商品ID={} 库存不足", productId);
            throw new RuntimeException("库存不足，手慢了！");
        }

        log.info("库存扣减成功: 商品ID={}", productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollback(Long productId, Integer count) {
        log.info("回滚库存: 商品ID={}, 数量={}", productId, count);
        stockMapper.rollbackStock(productId, count);
    }

    @Override
    public Integer getAvailableStock(Long productId) {
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getProductId, productId));
        return stock != null ? stock.getAvailableStock() : 0;
    }
}