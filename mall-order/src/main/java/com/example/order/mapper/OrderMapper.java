package com.example.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    @Select("SELECT * FROM t_order WHERE user_id = #{userId} AND product_id = #{productId} LIMIT 1")
    Order selectByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    @Select("SELECT * FROM t_order WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Order> selectByUserId(@Param("userId") Long userId);
}