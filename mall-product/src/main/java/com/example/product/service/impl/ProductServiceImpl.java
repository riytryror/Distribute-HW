package com.example.product.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.example.product.entity.Product;
import com.example.product.mapper.ProductMapper;
import com.example.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import java.util.concurrent.TimeUnit;

import java.util.List;
import java.util.Random;


@Service
@Slf4j
@DS("product") // 关键：指定操作 mall_product 数据库
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String CACHE_KEY_PREFIX = "seckill:product:";
    private static final String EMPTY_CACHE_VALUE = "{}";

    @Override
    public List<Product> listAll() {
        log.info("查询所有商品列表");
        return productMapper.selectList(null);
    }

    @Override
    public Product getById(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;

        // 1. 先从 Redis 查
        String productJson = redisTemplate.opsForValue().get(cacheKey);

        if (StrUtil.isNotBlank(productJson)) {
            // 防穿透判断：如果是之前存的空对象，直接返回 null
            if (EMPTY_CACHE_VALUE.equals(productJson)) {
                return null;
            }
            return JSONUtil.toBean(productJson, Product.class);
        }

        // 2. 缓存未命中，尝试加锁以防“缓存击穿”
        synchronized (this) {
            // 双检锁：拿到锁后再查一次 Redis
            productJson = redisTemplate.opsForValue().get(cacheKey);
            if (StrUtil.isNotBlank(productJson)) {
                if (EMPTY_CACHE_VALUE.equals(productJson)) return null;
                return JSONUtil.toBean(productJson, Product.class);
            }

            // 3. 真正查询数据库
            log.info("缓存未命中，开始查询数据库商品详情: id={}", id);
            Product product = productMapper.selectById(id);

            // 4. 防缓存穿透：数据库查不到，也缓存一个空对象
            if (product == null) {
                log.warn("数据库无此商品，缓存空值以防穿透: id={}", id);
                redisTemplate.opsForValue().set(cacheKey, EMPTY_CACHE_VALUE, 2, TimeUnit.MINUTES);
                return null;
            }

            // 5. 防缓存雪崩：设置随机过期时间
            long baseExpire = 30L;
            long randomMinutes = new Random().nextInt(10); // 随机 0-9 分钟
            
            redisTemplate.opsForValue().set(
                cacheKey, 
                JSONUtil.toJsonStr(product), 
                baseExpire + randomMinutes, 
                TimeUnit.MINUTES
            );

            return product;
        }
    }

    @Override
    public void createProduct(Product product) {
        productMapper.insert(product);
        log.info("新增商品成功: {}", product.getName());
        // 建议：新增商品后可以主动删除缓存或不处理（等待过期）
    }
}