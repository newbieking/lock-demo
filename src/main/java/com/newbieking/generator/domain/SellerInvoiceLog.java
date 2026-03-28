package com.newbieking.generator.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import lombok.Builder;
import lombok.Data;

/**
 * 发票操作日志表
 * @TableName seller_invoice_log
 */
@TableName(value ="seller_invoice_log")
@Data
@Builder
public class SellerInvoiceLog {
    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发票ID
     */
    private Long invoiceId;

    /**
     * 操作类型：INVALID-作废 RED-冲红
     */
    private String action;

    /**
     * 操作原因
     */
    private String content;

    /**
     * 
     */
    private Date createTime;
}