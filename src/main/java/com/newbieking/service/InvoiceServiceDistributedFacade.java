package com.newbieking.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class InvoiceServiceDistributedFacade {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 锁前缀，防止Key冲突
    public static final String LOCK_PREFIX = "lock:invoice:invalid:";
    public static final String IDEMPOTENT_PREFIX = "idempotent:invoice:";

    @Autowired
    private InvoiceServiceDistributed invoiceServiceDistributed;

    /**
     * Redisson分布式锁 - 集群安全，支持可重入、看门狗续期
     */
    public void invalidInvoice(Long invoiceId, String reason) {
        String lockKey = LOCK_PREFIX + invoiceId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = false;
        try {
            // 参数：等待时间、租约时间、时间单位
            // 租约时间-1表示启用看门狗（默认30秒，续期到30秒）
            locked = lock.tryLock(5, -1, TimeUnit.SECONDS);
            if (!locked) throw new RuntimeException("系统繁忙，请稍后重试");

            // 双重检查：获取锁后再次确认状态（防止锁等待期间已被处理）
            String idempotentKey = IDEMPOTENT_PREFIX + invoiceId;
            Boolean isNew = redisTemplate.opsForValue()
                    .setIfAbsent(idempotentKey, "processing", 24, TimeUnit.HOURS);

            if (Boolean.FALSE.equals(isNew)) {
                // 检查是否正在处理中（防悬挂）
                String status = redisTemplate.opsForValue().get(idempotentKey);
                if ("done".equals(status)) {
                    log.info("发票{}已处理完成（幂等拦截）", invoiceId);
                    return;
                }
                // 如果是"processing"，可能上次执行中断，继续执行（允许重入）
                log.warn("发票{}上次处理可能中断，继续执行", invoiceId);
            }

            invoiceServiceDistributed.doInvalidWithTransaction(invoiceId, reason, idempotentKey);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("操作中断");
        } finally {
            // 必须判断locked状态，避免unlock未获取的锁抛异常
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}