package com.example.order.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.example.common.constant.CacheKeys;
import com.example.common.dto.SeckillOrderMessage;
import com.example.common.dto.SeckillResultDTO;
import com.example.common.util.SnowflakeIdWorker;
import com.example.order.entity.Order;
import com.example.order.mapper.OrderMapper;
import com.example.order.service.OrderService;
import com.example.stock.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@DS("order") // 关键：指定此服务操作 mall_order 数据库
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private StockService stockService; // 注入库存模块的服务

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private KafkaTemplate<String, SeckillOrderMessage> kafkaTemplate;

    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(1, 1);
    private static final String RESULT_PENDING = "PENDING";
    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_FAIL = "FAIL";
    private static final long USER_MARK_TTL_SECONDS = 24 * 3600L;
    private static final long RESULT_TTL_SECONDS = 24 * 3600L;
    @Value("${seckill.kafka.topic-order-create:seckill-order-create}")
    private String orderTopic;

    private static final DefaultRedisScript<Long> PRE_DEDUCT_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('exists', KEYS[2]) == 1 then
                        return 2
                    end
                    local stock = redis.call('get', KEYS[1])
                    if not stock then
                        return -1
                    end
                    local stockNum = tonumber(stock)
                    if stockNum <= 0 then
                        return 0
                    end
                    redis.call('decr', KEYS[1])
                    redis.call('set', KEYS[2], ARGV[1], 'EX', ARGV[2])
                    redis.call('set', KEYS[3], ARGV[3], 'EX', ARGV[4])
                    return 1
                    """, Long.class);

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务
    public Long createOrder(Long userId, Long productId) {
        return doCreateOrder(userId, productId, idWorker.nextId());
    }

    @Override
    public Long submitSeckill(Long userId, Long productId) {
        ensureStockCache(productId);
        Long orderId = idWorker.nextId();
        String stockKey = stockKey(productId);
        String userMarkKey = userMarkKey(userId, productId);
        String resultKey = resultKey(userId, productId);

        Long result = redisTemplate.execute(
                PRE_DEDUCT_SCRIPT,
                List.of(stockKey, userMarkKey, resultKey),
                String.valueOf(orderId),
                String.valueOf(USER_MARK_TTL_SECONDS),
                encodeResult(RESULT_PENDING, orderId, "排队中"),
                String.valueOf(RESULT_TTL_SECONDS)
        );

        if (result == null) {
            throw new RuntimeException("秒杀预扣减失败，请重试");
        }
        if (result == 2L) {
            throw new RuntimeException("同一商品请勿重复秒杀");
        }
        if (result == 0L) {
            throw new RuntimeException("库存不足，手慢了");
        }
        if (result == -1L) {
            throw new RuntimeException("商品库存缓存未初始化");
        }

        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setOrderId(orderId);
        message.setUserId(userId);
        message.setProductId(productId);
        message.setCount(1);
        message.setRequestTime(System.currentTimeMillis());

        try {
            kafkaTemplate.send(orderTopic, String.valueOf(productId), message).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            clearUserMarkAndRefreshStock(userId, productId);
            saveSeckillResult(userId, productId, RESULT_FAIL, null, "消息投递失败");
            throw new RuntimeException("系统繁忙，请稍后重试");
        }
        return orderId;
    }

    @Override
    public void processSeckillOrder(SeckillOrderMessage message) {
        Long userId = message.getUserId();
        Long productId = message.getProductId();

        Order existed = orderMapper.selectByUserAndProduct(userId, productId);
        if (existed != null) {
            saveSeckillResult(userId, productId, RESULT_SUCCESS, existed.getId(), "秒杀成功");
            return;
        }

        boolean deducted = false;
        try {
            stockService.deduct(productId, 1);
            deducted = true;
            createOrderWithAssignedId(userId, productId, message.getOrderId(), message.getCount());
            saveSeckillResult(userId, productId, RESULT_SUCCESS, message.getOrderId(), "秒杀成功");
        } catch (DuplicateKeyException e) {
            if (deducted) {
                stockService.rollback(productId, 1);
            }
            Order duplicate = orderMapper.selectByUserAndProduct(userId, productId);
            if (duplicate != null) {
                saveSeckillResult(userId, productId, RESULT_SUCCESS, duplicate.getId(), "秒杀成功");
            } else {
                clearUserMarkAndRefreshStock(userId, productId);
                saveSeckillResult(userId, productId, RESULT_FAIL, null, "重复下单拦截");
            }
        } catch (Exception e) {
            if (deducted) {
                stockService.rollback(productId, 1);
            }
            clearUserMarkAndRefreshStock(userId, productId);
            saveSeckillResult(userId, productId, RESULT_FAIL, null, "下单失败");
            log.error("异步下单失败: orderId={}, userId={}, productId={}", message.getOrderId(), userId, productId, e);
        }
    }

    @Override
    public SeckillResultDTO querySeckillResult(Long userId, Long productId) {
        String raw = redisTemplate.opsForValue().get(resultKey(userId, productId));
        if (raw == null || raw.isBlank()) {
            return new SeckillResultDTO(RESULT_FAIL, null, "暂无秒杀记录");
        }
        return decodeResult(raw);
    }

    private Long doCreateOrder(Long userId, Long productId, Long orderId) {
        log.info("开始创建订单: 用户ID={}, 商品ID={}", userId, productId);

        Order existed = orderMapper.selectByUserAndProduct(userId, productId);
        if (existed != null) {
            log.warn("重复秒杀请求被拦截: 用户ID={}, 商品ID={}, 订单ID={}", userId, productId, existed.getId());
            throw new RuntimeException("请勿重复下单");
        }

        // 1. 调用库存服务扣减库存 (跨库调用)
        // stockService 内部使用了 @DS("stock")，会自动切换到 mall_stock 库
        // 如果库存不足，该方法会抛出 RuntimeException
        stockService.deduct(productId, 1);

        try {
            // 2. 扣减成功，创建订单记录
            createOrderWithAssignedId(userId, productId, orderId, 1);
            log.info("订单创建成功: 订单ID={}", orderId);
            return orderId;
        } catch (Exception e) {
            // 当前使用的是分库事务，订单库失败时显式补偿库存，避免出现库存与订单不一致
            stockService.rollback(productId, 1);
            throw e;
        }
    }

    private void createOrderWithAssignedId(Long userId, Long productId, Long orderId, Integer count) {
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setProductId(productId);
        order.setCount(count == null ? 1 : count);
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);
    }

    private void ensureStockCache(Long productId) {
        String stockKey = stockKey(productId);
        Boolean hasKey = redisTemplate.hasKey(stockKey);
        if (Boolean.TRUE.equals(hasKey)) {
            return;
        }
        Integer stock = stockService.getAvailableStock(productId);
        redisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(stock == null ? 0 : stock));
    }

    private void clearUserMarkAndRefreshStock(Long userId, Long productId) {
        redisTemplate.delete(userMarkKey(userId, productId));
        Integer latestStock = stockService.getAvailableStock(productId);
        redisTemplate.opsForValue().set(stockKey(productId), String.valueOf(latestStock == null ? 0 : latestStock));
    }

    private void saveSeckillResult(Long userId, Long productId, String status, Long orderId, String message) {
        redisTemplate.opsForValue().set(
                resultKey(userId, productId),
                encodeResult(status, orderId, message),
                RESULT_TTL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private String encodeResult(String status, Long orderId, String message) {
        return status + "|" + (orderId == null ? 0L : orderId) + "|" + message;
    }

    private SeckillResultDTO decodeResult(String raw) {
        String[] parts = raw.split("\\|", 3);
        if (parts.length < 3) {
            return new SeckillResultDTO(RESULT_FAIL, null, "结果解析失败");
        }
        Long orderId = Long.parseLong(parts[1]);
        return new SeckillResultDTO(parts[0], orderId == 0L ? null : orderId, parts[2]);
    }

    private String stockKey(Long productId) {
        return CacheKeys.SECKILL_STOCK_KEY_PREFIX + productId;
    }

    private String userMarkKey(Long userId, Long productId) {
        return CacheKeys.SECKILL_USER_MARK_KEY_PREFIX + userId + ":" + productId;
    }

    private String resultKey(Long userId, Long productId) {
        return CacheKeys.SECKILL_RESULT_KEY_PREFIX + userId + ":" + productId;
    }

    @Override
    public Order getById(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    @Override
    public List<Order> listByUserId(Long userId) {
        return orderMapper.selectByUserId(userId);
    }
}