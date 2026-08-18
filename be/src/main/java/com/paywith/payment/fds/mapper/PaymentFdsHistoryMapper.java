package com.paywith.payment.fds.mapper;

import com.paywith.payment.fds.domain.LastCompletedPayment;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 결제 FDS 판정용 read-only 조회. 거래·가맹점 테이블을 읽지만 쓰지는 않는다. */
@Mapper
public interface PaymentFdsHistoryMapper {

    /**
     * 이동 속도 단축평가의 기준점 — 같은 지갑의 24시간 내 최신 COMPLETED 결제 1건.
     * 결제는 평가 시점에 현재 거래 행이 아직 없으므로 자기 제외 파라미터가 필요 없다
     * (거래 행을 선생성하는 송금과 다른 점).
     */
    LastCompletedPayment findLastCompletedPayment(
        @Param("walletId") Long walletId,
        @Param("since") LocalDateTime since);

    /**
     * 분할 결제 룰: 윈도 내 상품권 의심(카테고리 목록 포함 && 단위 배수 금액) COMPLETED
     * 결제 수. COMPLETED 한정 — 분할 회피는 이전 건들이 통과에 성공했다는 뜻이다.
     */
    int countGiftCardSuspectPayments(
        @Param("walletId") Long walletId,
        @Param("since") LocalDateTime since,
        @Param("categories") List<String> categories,
        @Param("amountUnit") long amountUnit);

    /**
     * 위험 업종 반복 룰: 윈도 내 위험 업종(카테고리 목록) COMPLETED 결제 수. 상품권 의심
     * 집계와 달리 금액 단위 조건이 없다 — 위험 업종은 금액 형태와 무관하게 반복 자체가
     * 신호다. COMPLETED 한정 — 분할 회피는 이전 건들이 통과에 성공했다는 뜻이다.
     */
    int countRiskyCategoryPayments(
        @Param("walletId") Long walletId,
        @Param("since") LocalDateTime since,
        @Param("categories") List<String> categories);
}
