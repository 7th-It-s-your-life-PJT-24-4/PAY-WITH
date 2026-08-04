package com.paywith.payment.mapper;

import com.paywith.payment.domain.PaymentRequest;
import com.paywith.payment.domain.PaymentWallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentRequestMapper {

    /**
     * JWT userId 기준 역할 조회. 인증 파트(JwtTokenProvider) 무수정 원칙에 따라
     * 역할 검사는 결제 파트에서 DB 조회로 수행한다. 반환값은 users.role('WARD'/'GUARD').
     */
    String findUserRole(@Param("userId") Long userId);

    /** 피보호자의 ACTIVE 페어링 존재 여부 (WARD_001 판정용) */
    boolean existsActivePairing(@Param("seniorId") Long seniorId);

    /** 피보호자 지갑 조회 (읽기 전용 — WALLET_001/WALLET_002 판정용) */
    PaymentWallet findWalletByUserId(@Param("userId") Long userId);

    int insert(PaymentRequest paymentRequest);

    /** merchants·transactions LEFT JOIN 포함 상세 조회 (폴링 응답용) */
    PaymentRequest findById(@Param("paymentId") Long paymentId);

    /** merchants·transactions LEFT JOIN 포함 상세 조회 (완료·실패 건의 멱등 응답 조립용) */
    PaymentRequest findByToken(@Param("qrToken") String qrToken);

    /**
     * 결제 실행 진입 시 행잠금 조회(FOR UPDATE). 이 잠금 아래에서 상태·만료·소유자를 최종
     * 확인한 뒤 전이한다. 잠금 순서 규약: payment_requests를 wallets보다 먼저 잠근다.
     */
    PaymentRequest findByTokenForUpdate(@Param("qrToken") String qrToken);

    /**
     * 조건부 전이 PENDING→PROCESSING + 스캔 시점의 가맹점·금액 확정.
     * 반환값 0이면 만료·취소 등으로 상태가 선점된 것이므로 재조회로 판단한다.
     */
    int markProcessing(
        @Param("paymentId") Long paymentId,
        @Param("merchantId") Long merchantId,
        @Param("amount") Long amount
    );

    /** 조건부 전이 PROCESSING→COMPLETED + 원장 거래 연결(transaction_id UNIQUE) */
    int completePayment(@Param("paymentId") Long paymentId, @Param("transactionId") Long transactionId);

    /** 조건부 전이 PROCESSING→FAILED + 실패 사유 기록 (예: INSUFFICIENT_BALANCE) */
    int failPayment(@Param("paymentId") Long paymentId, @Param("failureCode") String failureCode);

    /**
     * lazy 만료 전이: PENDING이고 expires_at이 경과한 경우에만 EXPIRED로 갱신.
     * 반환값 0이면 다른 트랜잭션이 먼저 상태를 바꾼 것이므로 재조회로 최신 상태를 판단한다.
     */
    int markExpiredIfPending(@Param("paymentId") Long paymentId);

    /**
     * 일괄 만료 전이: PENDING이면서 expires_at이 경과한 모든 행을 EXPIRED로 갱신.
     * 만료 스캔(PaymentExpiryService) 전용 — 개별 건의 lazy 전이는 markExpiredIfPending을 쓴다.
     */
    int expireOverdue();

    /**
     * 조건부 취소: PENDING이고 아직 만료되지 않은 경우에만 CANCELED로 갱신.
     * 반환값 0이면 만료·스캔 등으로 상태가 선점된 것이므로 재조회로 판단한다.
     */
    int cancelIfPending(@Param("paymentId") Long paymentId);
}
