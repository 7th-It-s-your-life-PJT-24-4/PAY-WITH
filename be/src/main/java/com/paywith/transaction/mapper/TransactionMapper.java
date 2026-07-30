package com.paywith.transaction.mapper;

import com.paywith.charge.dto.ChargeDetailResponse;
import com.paywith.charge.dto.ChargeHistoryItem;
import com.paywith.transaction.domain.Transaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TransactionMapper {
    //거래 추가 -> ChargeServiceImpl, TransferServiceImpl에서 사용
    void insertTransaction(Transaction transaction);

    // 거래 상태 업데이트 (REQUESTED -> COMPLETED) TransferServiceImple에서
    int updateStatus(@Param("transactionId") Long transactionId, @Param("status") String status);

    // 거래 완료시 상태 업데이트
    int completeTransaction(
            @Param("transactionId") Long transactionId,
            @Param("status") String status,
            @Param("balanceAfter") Long balanceAfter,
            @Param("completedAt") LocalDateTime completedAt
    );

    //보호자 충전 내역
    List<ChargeHistoryItem> findChargeHistoriesByGuardId(Long guardId);

    //보호자 충전 내역 상세
    ChargeDetailResponse findChargeDetailByGuardId(
            @Param("transactionId") Long transactionId,
            @Param("guardId") Long guardId
    );


}
