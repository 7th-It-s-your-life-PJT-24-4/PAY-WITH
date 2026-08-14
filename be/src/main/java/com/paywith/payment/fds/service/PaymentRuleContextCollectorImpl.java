package com.paywith.payment.fds.service;

import com.paywith.fds.mapper.FdsHistoryMapper;
import com.paywith.merchant.domain.Merchant;
import com.paywith.payment.fds.mapper.PaymentFdsHistoryMapper;
import com.paywith.payment.fds.rule.PaymentRuleContext;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 결제 컨텍스트 수집. 실행 경로의 잠금(payment_requests FOR UPDATE) 이전에 호출되는
 * read-only 단계라 잠금 보유 시간에 영향을 주지 않는다.
 *
 * 승인 대기 조회만 송금 FDS의 {@link FdsHistoryMapper}를 재사용한다 — 판정 데이터가 같고
 * 읽기 전용 주입이라 송금 코드를 수정하지 않는다. 나머지 조회는 결제 전용 매퍼.
 */
@Service
public class PaymentRuleContextCollectorImpl implements PaymentRuleContextCollector {

    /**
     * 이동 속도 기준점 탐색 범위(시간). 설계 고정값 — 민감도 조정은 이 범위가 아니라
     * 속도 임계값(fds.payment.impossible-speed-kmh)으로 한다.
     */
    private static final int LAST_PAYMENT_WINDOW_HOURS = 24;

    /** DB에 기록되는 시각과 같은 시간대여야 윈도 계산이 어긋나지 않는다(결제 실행부와 동일). */
    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private final PaymentFdsHistoryMapper paymentFdsHistoryMapper;
    private final FdsHistoryMapper fdsHistoryMapper;
    private final int splitWindowMinutes;
    private final int riskyRepeatedWindowMinutes;
    private final List<String> riskyCategories;
    private final List<String> giftCardCategories;
    private final long giftCardAmountUnit;

    public PaymentRuleContextCollectorImpl(
        PaymentFdsHistoryMapper paymentFdsHistoryMapper,
        FdsHistoryMapper fdsHistoryMapper,
        @Value("${fds.payment.split.window-minutes}") int splitWindowMinutes,
        @Value("${fds.payment.risky-repeated.window-minutes}") int riskyRepeatedWindowMinutes,
        @Value("${fds.payment.risky-categories}") String[] riskyCategories,
        @Value("${fds.payment.gift-card-categories}") String[] giftCardCategories,
        @Value("${fds.payment.gift-card-amount-unit}") long giftCardAmountUnit
    ) {
        if (giftCardAmountUnit <= 0) {
            // 배수 판정(amount % unit)의 분모라 0 이하는 기동 시점에 거른다.
            throw new IllegalArgumentException(
                "fds.payment.gift-card-amount-unit 은 양수여야 합니다: " + giftCardAmountUnit);
        }
        this.paymentFdsHistoryMapper = paymentFdsHistoryMapper;
        this.fdsHistoryMapper = fdsHistoryMapper;
        this.splitWindowMinutes = splitWindowMinutes;
        this.riskyRepeatedWindowMinutes = riskyRepeatedWindowMinutes;
        this.riskyCategories = Arrays.asList(riskyCategories);
        this.giftCardCategories = Arrays.asList(giftCardCategories);
        this.giftCardAmountUnit = giftCardAmountUnit;
    }

    @Override
    public PaymentRuleContext collect(Long walletId, Long amount, Merchant merchant) {
        LocalDateTime now = LocalDateTime.now(ZONE_SEOUL);

        return PaymentRuleContext.builder()
            .amount(amount)
            .requestedAt(now)
            .merchantCategoryCode(merchant.getCategoryCode())
            .merchantLatitude(merchant.getLatitude())
            .merchantLongitude(merchant.getLongitude())
            .lastCompletedPayment(paymentFdsHistoryMapper.findLastCompletedPayment(
                walletId, now.minusHours(LAST_PAYMENT_WINDOW_HOURS)))
            .giftCardSuspectRecentCount(countGiftCardSuspects(walletId, now))
            .riskyCategoryRecentCount(countRiskyRepeats(walletId, now))
            .pendingApprovalExists(fdsHistoryMapper.existsPendingApproval(walletId))
            .riskyCategories(riskyCategories)
            .giftCardCategories(giftCardCategories)
            .giftCardAmountUnit(giftCardAmountUnit)
            .build();
    }

    /** 목록이 비면 IN 절이 성립하지 않으므로 쿼리 없이 0으로 둔다. */
    private int countGiftCardSuspects(Long walletId, LocalDateTime now) {
        if (giftCardCategories.isEmpty()) {
            return 0;
        }
        return paymentFdsHistoryMapper.countGiftCardSuspectPayments(
            walletId, now.minusMinutes(splitWindowMinutes), giftCardCategories, giftCardAmountUnit);
    }

    /** 목록이 비면 IN 절이 성립하지 않으므로 쿼리 없이 0으로 둔다. */
    private int countRiskyRepeats(Long walletId, LocalDateTime now) {
        if (riskyCategories.isEmpty()) {
            return 0;
        }
        return paymentFdsHistoryMapper.countRiskyCategoryPayments(
            walletId, now.minusMinutes(riskyRepeatedWindowMinutes), riskyCategories);
    }
}
