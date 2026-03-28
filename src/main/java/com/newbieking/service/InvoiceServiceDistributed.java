package com.newbieking.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.newbieking.generator.domain.SellerAccount;
import com.newbieking.generator.domain.SellerInvoice;
import com.newbieking.generator.domain.SellerInvoiceLog;
import com.newbieking.generator.mapper.SellerAccountMapper;
import com.newbieking.generator.mapper.SellerInvoiceLogMapper;
import com.newbieking.generator.mapper.SellerInvoiceMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ConcurrentModificationException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class InvoiceServiceDistributed {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SellerInvoiceMapper sellerInvoiceMapper;
    @Autowired
    private SellerAccountMapper sellerAccountMapper;
    @Autowired
    private SellerInvoiceLogMapper sellerInvoiceLogMapper;


    /**
     * 事务包装：锁内开启事务，保证事务提交前锁不释放
     * 关键点：@Transactional在锁内，事务提交后才unlock
     */
    @Transactional(rollbackFor = Exception.class)
    public void doInvalidWithTransaction(Long invoiceId, String reason, String idempotentKey) {
        try {
            // 数据库幂等检查（最终一致性保障）
            SellerInvoice invoice = sellerInvoiceMapper.selectById(invoiceId);
            // 已处理则直接标记完成，不删除Redis键
            if (invoice == null || invoice.getStatus() == 2) {
                log.info("发票{}已作废或不存在，标记完成", invoiceId);
                redisTemplate.opsForValue().set(idempotentKey, "done", 24, TimeUnit.HOURS);
                return;
            }

            // 状态校验：必须是已开票(1)才能作废
            // 状态转移
            if (invoice.getStatus() != 1) {
                throw new IllegalStateException("发票状态不正确，当前状态：" + invoice.getStatus());
            }

            // 业务操作...
            updateStatusAndDeduct(invoice, reason);

            // 标记处理完成
            redisTemplate.opsForValue().set(idempotentKey, "done", 24, TimeUnit.HOURS);

        } catch (Exception e) {
            // 失败时删除Redis标记，允许重试（除非已明确完成）
            if (!(e instanceof IllegalStateException)) {
                redisTemplate.delete(idempotentKey);
            }
            throw e;
        }

    }

    private void updateStatusAndDeduct(SellerInvoice invoice, String reason) {

        int rows = sellerInvoiceMapper.updateStatus(invoice.getSellerId(), invoice.getId(),
                1,  // 正常
                2, // 作废
                invoice.getVersion());
        if (rows == 0) throw new ConcurrentModificationException("发票状态已被修改");
        SellerAccount account = sellerAccountMapper.selectOne(Wrappers.<SellerAccount>lambdaQuery()
                .eq(SellerAccount::getSellerId, invoice.getSellerId()));
        if (account == null) {
            throw new RuntimeException("卖家账户不存在");
        }
        int deducted = sellerAccountMapper.deductBalance(
                invoice.getSellerId(),
                invoice.getAmount(),
                account.getVersion());
        if (deducted == 0) throw new ConcurrentModificationException("余额不足或账户并发修改");

        sellerInvoiceLogMapper.insert(SellerInvoiceLog.builder()
                .invoiceId(invoice.getId())
                .action("INVALID")
                .content(reason)
                .build());

    }
}