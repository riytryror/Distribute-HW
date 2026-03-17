package com.example.product.service;

import com.example.product.entity.Product;
import java.util.List;

public interface ProductService {
    /**
     * 获取所有商品列表
     */
    List<Product> listAll();

    /**
     * 根据ID获取商品详情
     */
    Product getById(Long id);

    /**
     * 新增商品（供后台或初始化数据使用）
     */
    void createProduct(Product product);
}