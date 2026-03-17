package com.example.api.controller;

import com.example.common.result.Result;
import com.example.product.entity.Product;
import com.example.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 获取商品列表
     * 对应接口文档：GET /api/v1/products
     */
    @GetMapping
    public Result<List<Product>> list() {
        return Result.success(productService.listAll());
    }

    /**
     * 获取商品详情
     * 对应接口文档：GET /api/v1/products/{id}
     */
    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }
        return Result.success(product);
    }
}