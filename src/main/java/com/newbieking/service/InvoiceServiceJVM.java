package com.newbieking.service;

import com.newbieking.generator.domain.SellerInvoice;
import com.newbieking.generator.domain.SellerInvoiceLog;
import com.newbieking.generator.mapper.SellerAccountMapper;
import com.newbieking.generator.mapper.SellerInvoiceLogMapper;
import com.newbieking.generator.mapper.SellerInvoiceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ConcurrentModificationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class InvoiceServiceJVM {

    // 缺点：锁粒度粗，所有发票共用一把锁；或锁粒度细，但JVM重启丢失
    private final ConcurrentHashMap<Long, ReentrantLock> invoiceLocks = new ConcurrentHashMap<>();

    @Autowired
    private SellerInvoiceMapper invoiceMapper;
    @Autowired
    private SellerAccountMapper accountMapper;
    @Autowired
    private SellerInvoiceLogMapper logMapper;

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
            doInvalid(invoiceId, reason);
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

    @Transactional(rollbackFor = Exception.class)
    protected void doInvalid(Long invoiceId, String reason) {
        // 1. 幂等检查（状态机）
        SellerInvoice invoice = invoiceMapper.selectById(invoiceId);
        if (invoice.getStatus() == 2) { // 2=已作废
            log.warn("发票{}已作废，跳过重复操作", invoiceId);
            return; // 幂等返回，不抛异常（或根据业务决定）
        }

        // 2. 乐观锁二次防护（数据库层面）
        int rows = invoiceMapper.updateStatus(invoice.getSellerId(), invoiceId, 1, 2, invoice.getVersion());
        if (rows == 0) throw new ConcurrentModificationException("状态已被修改");

        // 3. 金额扣减（原子SQL，带余额校验）
        int deducted = accountMapper.deductBalance(
                invoice.getSellerId(),
                invoice.getAmount(),
                invoice.getVersion() // 作为version/条件
        );
        if (deducted == 0) throw new RuntimeException("余额不足或账户并发修改");

        // 4. 记录日志
        logMapper.insert(SellerInvoiceLog.builder()
                .invoiceId(invoiceId)
                .action("INVALID")
                .content(reason)
                .build()
        );
    }
}