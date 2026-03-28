package com.newbieking.generator.mapper;

import com.newbieking.generator.domain.SellerInvoice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
* @author nb
* @description 针对表【seller_invoice(发票信息表)】的数据库操作Mapper
* @createDate 2026-03-26 20:59:02
* @Entity generator.domain.SellerInvoice
*/
@Mapper
public interface SellerInvoiceMapper extends BaseMapper<SellerInvoice> {

    int updateStatus(
            @Param("sellerId") Long sellerId,
            @Param("invoiceId") Long invoiceId,
            @Param("oldStatus") int oldStatus,
            @Param("newStatus") int newStatus,
            @Param("version") Integer version);

    SellerInvoice selectForUpdate(@Param("invoiceId") Long invoiceId);
}




