package com.paywith.recipient.mapper;

import com.paywith.bank.dto.BankResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BankMapper {
    List<BankResponse> findAllActive();

    // 은행 코드로 은행명 조회
    String findBankName(@Param("bankCode") String bankCode);
}
