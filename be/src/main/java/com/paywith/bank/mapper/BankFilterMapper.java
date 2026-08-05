package com.paywith.bank.mapper;

import com.paywith.bank.domain.Bank;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BankFilterMapper {

    List<Bank> findAllActive();

    // 피보호자의 ACTIVE 페어링 존재 여부 (WARD_001 판정용, 다른 도메인 매퍼와 독립적으로 자체 보유)
    boolean existsActivePairing(@Param("wardId") Long wardId);
}