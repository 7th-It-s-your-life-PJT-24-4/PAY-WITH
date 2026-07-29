package com.paywith.fds.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * transactions.risk_score 갱신 전용. 이 컬럼의 single writer 는 FDS 이고,
 * risk_evaluations insert 와 같은 트랜잭션에서만 갱신해야 두 값이 어긋나지 않는다.
 */
@Mapper
public interface TransactionRiskMapper {

    int updateRiskScore(@Param("transactionId") Long transactionId, @Param("riskScore") int riskScore);
}
