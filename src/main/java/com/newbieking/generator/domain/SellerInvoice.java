package com.newbieking.generator.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 发票信息表
 * @TableName seller_invoice
 */
@TableName(value ="seller_invoice")
@Data
public class SellerInvoice {
    /**
     * 发票ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商户ID
     */
    private Long sellerId;

    /**
     * 发票号码
     */
    private String invoiceNo;

    /**
     * 发票金额
     */
    private BigDecimal amount;

    /**
     * 状态：1-正常 2-已作废 3-已冲红
     */
    private Integer status;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;
}