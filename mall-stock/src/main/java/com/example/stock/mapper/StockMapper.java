package com.example.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    /**
     * 核心：扣减库存 SQL
     * 通过 WHERE 条件判断 available_stock >= #{count} 来防止超卖
     * MySQL 会对该行加排他锁，保证并发下的数据安全
     */
    @Update("UPDATE t_stock SET available_stock = available_stock - #{count} " +
            "WHERE product_id = #{productId} AND available_stock >= #{count}")
    int decreaseStock(@Param("productId") Long productId, @Param("count") Integer count);

    /**
     * 库存回滚（用于订单取消或支付超时）
     */
    @Update("UPDATE t_stock SET available_stock = available_stock + #{count} " +
            "WHERE product_id = #{productId}")
    int rollbackStock(@Param("productId") Long productId, @Param("count") Integer count);
}