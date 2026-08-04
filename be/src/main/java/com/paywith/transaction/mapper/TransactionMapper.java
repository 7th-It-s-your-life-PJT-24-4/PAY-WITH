package com.paywith.transaction.mapper;

import com.paywith.charge.dto.ChargeDetailResponse;
import com.paywith.charge.dto.ChargeHistoryItem;
import com.paywith.transaction.domain.Transaction;
import com.paywith.transfer.dto.TransferExecutionContext;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TransactionMapper {
    //거래 추가 -> ChargeServiceImpl, TransferServiceImpl에서 사용
    void insertTransaction(Transaction transaction);

    // 거래 상태 업데이트 (REQUESTED -> COMPLETED) TransferServiceImpl에서
    int updateStatus(@Param("transactionId") Long transactionId, @Param("status") String status);

    // 거래 실패 시 상태 업데이트 (REQUESTED -> FAILED) 잔액 부족 등 finalize 단계 실패
    // (HELD/APPROVED 등 다른 경로로 이미 진행된 거래는 건드리지 않음)
    int markFailedIfRequested(@Param("transactionId") Long transactionId);

    // 거래 완료시 상태 업데이트
    int completeTransaction(
            @Param("transactionId") Long transactionId,
            @Param("status") String status,
            @Param("balanceAfter") Long balanceAfter,
            @Param("completedAt") LocalDateTime completedAt
    );

    // 보호자 승인 후속 처리(HELD 시점엔 PreparedTransfer이 없으니까) - transactionId로 잔액차감/입금에 필요한 정보 재조회 (지갑, 수취인, 금액 등)
    TransferExecutionContext findExecutionContextByTransactionId(@Param("transactionId") Long transactionId);

    //보호자 충전 내역
    List<ChargeHistoryItem> findChargeHistoriesByGuardId(Long guardId);

    //보호자 충전 내역 상세
    ChargeDetailResponse findChargeDetailByGuardId(
            @Param("transactionId") Long transactionId,
            @Param("guardId") Long guardId
    );


}
