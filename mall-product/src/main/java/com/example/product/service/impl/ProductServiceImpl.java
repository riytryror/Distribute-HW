package com.example.product.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.example.common.constant.CacheKeys;
import com.example.product.entity.Product;
import com.example.product.mapper.ProductMapper;
import com.example.product.service.ProductService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@DS("product_master") // 默认写入走主库
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String EMPTY_CACHE_VALUE = "{}";
    private static final long EMPTY_CACHE_TTL_MINUTES = 2L;
    private static final long BASE_CACHE_TTL_MINUTES = 30L;
    private static final long LOCK_TTL_SECONDS = 8L;

    @Override
    @DS("product") // 读流量走 product 组（主从负载）
    public List<Product> listAll() {
        log.info("查询所有商品列表（读组 product）");
        return productMapper.selectList(null);
    }

    @Override
    @DS("product") // 读流量走 product 组（主从负载）
    public Product getById(Long id) {
        String cacheKey = CacheKeys.PRODUCT_DETAIL_KEY_PREFIX + id;
        Product cached = readFromCache(cacheKey);
        if (cached != null || EMPTY_CACHE_VALUE.equals(redisTemplate.opsForValue().get(cacheKey))) {
            return cached;
        }

        String lockKey = CacheKeys.PRODUCT_LOCK_KEY_PREFIX + id;
        String lockValue = UUID.randomUUID().toString();
        boolean lockAcquired = tryAcquireLock(lockKey, lockValue);

        if (lockAcquired) {
            try {
                Product doubleCheck = readFromCache(cacheKey);
                if (doubleCheck != null || EMPTY_CACHE_VALUE.equals(redisTemplate.opsForValue().get(cacheKey))) {
                    return doubleCheck;
                }
                return queryAndCacheFromDb(id, cacheKey);
            } finally {
                releaseLock(lockKey, lockValue);
            }
        }

        // 未拿到分布式锁时短暂等待，给持锁线程回填缓存时间
        for (int i = 0; i < 5; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            Product retry = readFromCache(cacheKey);
            if (retry != null || EMPTY_CACHE_VALUE.equals(redisTemplate.opsForValue().get(cacheKey))) {
                return retry;
            }
        }

        // 极端情况下兜底回源数据库，避免请求一直失败
        return queryAndCacheFromDb(id, cacheKey);
    }

    @Override
    public void createProduct(Product product) {
        log.info("新增商品走主库（product_master）: {}", product.getName());
        productMapper.insert(product);
        if (product.getId() != null) {
            redisTemplate.delete(CacheKeys.PRODUCT_DETAIL_KEY_PREFIX + product.getId());
        }
    }

    private Product readFromCache(String cacheKey) {
        String productJson = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isBlank(productJson)) {
            return null;
        }
        if (EMPTY_CACHE_VALUE.equals(productJson)) {
            return null;
        }
        return JSONUtil.toBean(productJson, Product.class);
    }

    private Product queryAndCacheFromDb(Long id, String cacheKey) {
        log.info("缓存未命中，回源数据库查询商品详情: id={}", id);
        Product product = productMapper.selectById(id);
        if (product == null) {
            redisTemplate.opsForValue().set(cacheKey, EMPTY_CACHE_VALUE, EMPTY_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            return null;
        }

        long ttl = BASE_CACHE_TTL_MINUTES + ThreadLocalRandom.current().nextLong(10);
        redisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(product), ttl, TimeUnit.MINUTES);
        return product;
    }

    private boolean tryAcquireLock(String lockKey, String lockValue) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    private void releaseLock(String lockKey, String lockValue) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
        script.setResultType(Long.class);
        redisTemplate.execute(script, List.of(lockKey), lockValue);
    }
}