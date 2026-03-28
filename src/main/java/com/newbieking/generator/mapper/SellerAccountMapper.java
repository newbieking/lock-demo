package com.newbieking.generator.mapper;

import com.newbieking.generator.domain.SellerAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
* @author nb
* @description 针对表【seller_account(商户账户表)】的数据库操作Mapper
* @createDate 2026-03-26 20:59:02
* @Entity generator.domain.SellerAccount
*/
@Mapper
public interface SellerAccountMapper extends BaseMapper<SellerAccount> {

    int deductBalance(
            @Param("sellerId") Long sellerId,
            @Param("amount") BigDecimal amount,
            @Param("version") Integer version);
}




