package com.newbieking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class InvoiceServiceJVMFacade {

    // 缺点：锁粒度粗，所有发票共用一把锁；或锁粒度细，但JVM重启丢失
    private final ConcurrentHashMap<Long, ReentrantLock> invoiceLocks = new ConcurrentHashMap<>();


    @Autowired
    private InvoiceServiceJVM invoiceServiceJVM;

    /**
     * JVM锁版本 - 单机够用，集群失效
     */
    public void invalidInvoice(Long invoiceId, String reason) {
        // 每个发票ID对应一把锁（细粒度）
        ReentrantLock lock = invoiceLocks.computeIfAbsent(invoiceId, k -> new ReentrantLock());

        boolean locked = false;
        try {
            // 3秒超时，防止死锁
            locked = lock.tryLock(3, TimeUnit.SECONDS);
            if (!locked) throw new RuntimeException("获取锁超时，请稍后重试");

            // ========== 临界区开始 ==========
            invoiceServiceJVM.doInvalid(invoiceId, reason);
            // ========== 临界区结束 ==========

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("操作被中断");
        } finally {
            if (locked) lock.unlock();
            // 可选：长期无竞争的锁清理
            if (!lock.hasQueuedThreads()) {
                invoiceLocks.remove(invoiceId, lock);
            }
        }
    }

}