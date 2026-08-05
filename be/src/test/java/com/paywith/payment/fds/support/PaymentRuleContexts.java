package com.paywith.payment.fds.support;

import com.paywith.payment.fds.domain.LastCompletedPayment;
import com.paywith.payment.fds.rule.PaymentRuleContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 관심 없는 필드는 정상 결제 기본값으로 채운다. */
public final class PaymentRuleContexts {

    public static final List<String> RISKY_CATEGORIES =
        List.of("JEWELRY", "ELECTRONICS", "GIFT_CARD", "LUXURY");
    public static final List<String> GIFT_CARD_CATEGORIES = List.of("CVS", "MART");
    public static final long GIFT_CARD_AMOUNT_UNIT = 5_000L;

    /** dev-payment-seed 9002(금은방)와 같은 서울 종로 좌표. */
    public static final BigDecimal SEOUL_LAT = new BigDecimal("37.5700000");
    public static final BigDecimal SEOUL_LNG = new BigDecimal("126.9850000");

    /** dev-payment-seed 9004(부산식당) 좌표 — 서울과 약 325km 거리. */
    public static final BigDecimal BUSAN_LAT = new BigDecimal("35.1587000");
    public static final BigDecimal BUSAN_LNG = new BigDecimal("129.1604000");

    private PaymentRuleContexts() {
    }

    /**
     * 주간에 중립 업종(어느 카테고리 목록에도 없음)에서 소액(5,000 비배수)을 결제하는,
     * 어떤 룰도 발동하지 않는 기준 컨텍스트. 직전 결제 이력도 없다.
     */
    public static PaymentRuleContext.PaymentRuleContextBuilder normal() {
        return PaymentRuleContext.builder()
            .amount(31_000L)
            .requestedAt(at(14))
            .merchantCategoryCode("RESTAURANT")
            .merchantLatitude(SEOUL_LAT)
            .merchantLongitude(SEOUL_LNG)
            .lastCompletedPayment(null)
            .giftCardSuspectRecentCount(0)
            .pendingApprovalExists(false)
            .riskyCategories(RISKY_CATEGORIES)
            .giftCardCategories(GIFT_CARD_CATEGORIES)
            .giftCardAmountUnit(GIFT_CARD_AMOUNT_UNIT);
    }

    public static LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 8, 5, hour, 0);
    }

    public static LastCompletedPayment lastPayment(
        BigDecimal latitude, BigDecimal longitude, LocalDateTime completedAt
    ) {
        LastCompletedPayment last = new LastCompletedPayment();
        last.setLatitude(latitude);
        last.setLongitude(longitude);
        last.setCompletedAt(completedAt);
        return last;
    }
}
