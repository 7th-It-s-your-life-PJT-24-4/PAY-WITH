package com.paywith.transaction.mapper;

import com.paywith.charge.dto.ChargeDetailResponse;
import com.paywith.charge.dto.ChargeHistoryItem;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.guard.dto.RecentTransactionResponse;
import com.paywith.transaction.domain.Transaction;
import com.paywith.transaction.domain.TransactionStatus;
import com.paywith.transaction.domain.TransactionType;
import com.paywith.transaction.dto.GuardTransactionDetailResponse;
import com.paywith.transaction.dto.GuardTransactionHistoryItem;
import com.paywith.transaction.dto.TransactionDetailResponse;
import com.paywith.transaction.dto.TransactionHistoryItem;
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
    int updateStatus(@Param("transactionId") Long transactionId, @Param("status") TransactionStatus status);

    // 거래 실패 시 상태 업데이트 (REQUESTED -> FAILED) 잔액 부족 등 finalize 단계 실패
    // (HELD/APPROVED 등 다른 경로로 이미 진행된 거래는 건드리지 않음)
    int markFailedIfRequested(@Param("transactionId") Long transactionId);

    // 거래 완료시 상태 업데이트
    int completeTransaction(
            @Param("transactionId") Long transactionId,
            @Param("status") TransactionStatus status,
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

    // 피보호자 본인의 거래 내역 목록
    List<TransactionHistoryItem> findMyTransactions(
            @Param("wardId") Long wardId,
            @Param("type") TransactionType type,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size
    );

    // 피보호자 목록 조회 총 건 count
    int countMyTransactions(
            @Param("wardId") Long wardId,
            @Param("type") TransactionType type,
            @Param("keyword") String keyword
    );

    // 피보호자 본인 거래 상세
    TransactionDetailResponse findMyTransactionDetail(
            @Param("transactionId") Long transactionId,
            @Param("wardId") Long wardId
    );

    // 보호자용 피보호자 거래 상세
    GuardTransactionDetailResponse findWardTransactionDetail(
            @Param("transactionId") Long transactionId,
            @Param("wardId") Long wardId
    );

    // 위험 판단 사유 목록
    List<String> findRiskReasons(@Param("transactionId") Long transactionId);

    // LLM 사후 재검토 요약
    String findLlmSummary(@Param("transactionId") Long transactionId);

    // 보호자가 피보호자의 거래 내역 조회
    List<GuardTransactionHistoryItem> findWardTransactions(
            @Param("guardId") Long guardId,
            @Param("wardId") Long wardId,
            @Param("type") TransactionType type,
            @Param("riskLevel") RiskLevel riskLevel,
            @Param("offset") int offset,
            @Param("size") int size
    );

    // 보호자가 피보호자의 거래 내역 총 건 조회
    int countWardTransactions(
            @Param("guardId") Long guardId,
            @Param("wardId") Long wardId,
            @Param("type") TransactionType type,
            @Param("riskLevel") RiskLevel riskLevel
    );

    // 보호자 홈 화면용 최근 거래 목록(종결 상태만, 최신순)
    List<RecentTransactionResponse> findRecentByWardId(
            @Param("wardId") Long wardId,
            @Param("limit") int limit
    );

    // 취소 대상 조회 -> 승인 대기 중 피보호자가 직접 거래 취소 (Approval 도메인과 연관)
    TransactionStatus findTransferStatusForCancel(@Param("transactionId") Long transactionId, @Param("wardId") Long wardId);

}
