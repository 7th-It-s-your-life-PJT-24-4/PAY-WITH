package com.paywith.payment.mapper;

import com.paywith.payment.domain.PaymentRequest;
import com.paywith.payment.domain.PaymentWallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentRequestMapper {

    /**
     * JWT userId 기준 역할 조회. 인증 파트(JwtTokenProvider) 무수정 원칙에 따라
     * 역할 검사는 결제 파트에서 DB 조회로 수행한다(B6). 반환값은 users.role('SENIOR'/'GUARD').
     */
    String findUserRole(@Param("userId") Long userId);

    /** 피보호자의 ACTIVE 페어링 존재 여부 (WARD_001 판정용) */
    boolean existsActivePairing(@Param("seniorId") Long seniorId);

    /** 피보호자 지갑 조회 (읽기 전용 — WALLET_001/WALLET_002 판정용) */
    PaymentWallet findWalletByUserId(@Param("userId") Long userId);

    int insert(PaymentRequest paymentRequest);

    /** merchants·transactions LEFT JOIN 포함 상세 조회 (폴링 응답용) */
    PaymentRequest findById(@Param("paymentId") Long paymentId);

    PaymentRequest findByToken(@Param("qrToken") String qrToken);

    /**
     * lazy 만료 전이: PENDING이고 expires_at이 경과한 경우에만 EXPIRED로 갱신.
     * 반환값 0이면 다른 트랜잭션이 먼저 상태를 바꾼 것이므로 재조회로 최신 상태를 판단한다.
     */
    int markExpiredIfPending(@Param("paymentId") Long paymentId);

    /**
     * 조건부 취소: PENDING이고 아직 만료되지 않은 경우에만 CANCELED로 갱신.
     * 반환값 0이면 만료·스캔 등으로 상태가 선점된 것이므로 재조회로 판단한다.
     */
    int cancelIfPending(@Param("paymentId") Long paymentId);
}
