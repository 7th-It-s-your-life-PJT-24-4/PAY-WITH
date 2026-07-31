package com.paywith.recipient.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BankMapper {
    // 은행 코드로 은행명 조회
    String findBankName(@Param("bankCode") String bankCode);
}
