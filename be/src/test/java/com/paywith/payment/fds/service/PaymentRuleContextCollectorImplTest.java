package com.paywith.payment.fds.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.paywith.fds.mapper.FdsHistoryMapper;
import com.paywith.merchant.domain.Merchant;
import com.paywith.payment.fds.domain.LastCompletedPayment;
import com.paywith.payment.fds.mapper.PaymentFdsHistoryMapper;
import com.paywith.payment.fds.rule.PaymentRuleContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("결제 FDS 판정 입력 수집")
class PaymentRuleContextCollectorImplTest {

    // walletId 와 amount 는 둘 다 Long 이라 뒤바꿔 넘겨도 컴파일이 통과한다.
    // 서로 다른 상수로 두어 매퍼 호출 인자 단언에서 뒤바뀜이 드러나게 한다.
    private static final long WALLET_ID = 200L;
    private static final long AMOUNT = 31_000L;

    // 두 윈도우를 일부러 다른 값으로 둔다. 운영 설정은 둘 다 30분이라
    // (fds.payment.split.window-minutes, fds.payment.risky-repeated.window-minutes)
    // 같은 값으로 두면 두 윈도우를 뒤바꿔 넘기는 실수를 잡을 수 없다.
    private static final int SPLIT_WINDOW_MINUTES = 10;
    private static final int RISKY_REPEATED_WINDOW_MINUTES = 30;

    /** 이동 속도 기준점 탐색 범위 — 수집기의 설계 고정값과 같은 24시간. */
    private static final int LAST_PAYMENT_WINDOW_HOURS = 24;

    // 두 카테고리 목록도 둘 다 List<String> 이라 뒤바꿔 넘겨도 컴파일이 통과한다.
    // 서로 다른 내용으로 두어 집계 쿼리가 제 목록을 받는지 단언한다.
    private static final String[] RISKY_CATEGORIES = {"JEWELRY", "ELECTRONICS"};
    private static final String[] GIFT_CARD_CATEGORIES = {"CVS", "MART"};
    private static final long GIFT_CARD_AMOUNT_UNIT = 5_000L;

    /** cutoff 는 수집기가 Asia/Seoul 로 계산하므로 범위 단언도 같은 시간대로 찍는다. */
    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private PaymentFdsHistoryMapper paymentFdsHistoryMapper;
    @Mock
    private FdsHistoryMapper fdsHistoryMapper;

    private PaymentRuleContextCollectorImpl collector(
        String[] riskyCategories, String[] giftCardCategories
    ) {
        return new PaymentRuleContextCollectorImpl(
            paymentFdsHistoryMapper,
            fdsHistoryMapper,
            SPLIT_WINDOW_MINUTES,
            RISKY_REPEATED_WINDOW_MINUTES,
            riskyCategories,
            giftCardCategories,
            GIFT_CARD_AMOUNT_UNIT);
    }

    private PaymentRuleContextCollectorImpl collector() {
        return collector(RISKY_CATEGORIES, GIFT_CARD_CATEGORIES);
    }

    private Merchant merchant(String categoryCode) {
        Merchant merchant = new Merchant();
        merchant.setMerchantId(9902L);
        merchant.setCategoryCode(categoryCode);
        merchant.setLatitude(new BigDecimal("37.5700000"));
        merchant.setLongitude(new BigDecimal("126.9850000"));
        return merchant;
    }

    // 배수 판정(amount % unit)의 분모라 0 이하는 기동 시점에 걸러져야 한다.
    @Test
    void constructor_rejectsNonPositiveGiftCardAmountUnit() {
        assertThatThrownBy(() -> new PaymentRuleContextCollectorImpl(
                paymentFdsHistoryMapper, fdsHistoryMapper,
                SPLIT_WINDOW_MINUTES, RISKY_REPEATED_WINDOW_MINUTES,
                RISKY_CATEGORIES, GIFT_CARD_CATEGORIES, 0L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("gift-card-amount-unit");
    }

    @Test
    void collect_mapsRequestAndMerchantFieldsIntoContext() {
        LastCompletedPayment last = new LastCompletedPayment();
        given(paymentFdsHistoryMapper.findLastCompletedPayment(
                eq(WALLET_ID), any(LocalDateTime.class)))
            .willReturn(last);
        given(paymentFdsHistoryMapper.countGiftCardSuspectPayments(
                eq(WALLET_ID), any(LocalDateTime.class), anyList(), anyLong()))
            .willReturn(2);
        given(paymentFdsHistoryMapper.countRiskyCategoryPayments(
                eq(WALLET_ID), any(LocalDateTime.class), anyList()))
            .willReturn(3);
        given(fdsHistoryMapper.existsPendingApproval(WALLET_ID)).willReturn(true);
        LocalDateTime beforeCall = LocalDateTime.now(ZONE_SEOUL);

        PaymentRuleContext context = collector().collect(WALLET_ID, AMOUNT, merchant("JEWELRY"));

        assertThat(context.getAmount()).isEqualTo(AMOUNT);
        assertThat(context.getRequestedAt()).isBetween(beforeCall, LocalDateTime.now(ZONE_SEOUL));
        assertThat(context.getMerchantCategoryCode()).isEqualTo("JEWELRY");
        assertThat(context.getMerchantLatitude()).isEqualByComparingTo("37.5700000");
        assertThat(context.getMerchantLongitude()).isEqualByComparingTo("126.9850000");
        assertThat(context.getLastCompletedPayment()).isSameAs(last);
        assertThat(context.getGiftCardSuspectRecentCount()).isEqualTo(2);
        assertThat(context.getRiskyCategoryRecentCount()).isEqualTo(3);
        assertThat(context.isPendingApprovalExists()).isTrue();
        assertThat(context.getRiskyCategories()).containsExactly(RISKY_CATEGORIES);
        assertThat(context.getGiftCardCategories()).containsExactly(GIFT_CARD_CATEGORIES);
        assertThat(context.getGiftCardAmountUnit()).isEqualTo(GIFT_CARD_AMOUNT_UNIT);
    }

    // cutoff 시각은 collect() 안의 now 로 정해져 정확한 값을 알 수 없다.
    // 호출 전후로 시각을 찍어 범위로 단언한다.
    @Test
    void collect_appliesSplitWindowToGiftCardSuspectCutoff() {
        LocalDateTime beforeCall = LocalDateTime.now(ZONE_SEOUL);

        collector().collect(WALLET_ID, AMOUNT, merchant("CVS"));

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        then(paymentFdsHistoryMapper).should().countGiftCardSuspectPayments(
            eq(WALLET_ID), since.capture(),
            eq(List.of(GIFT_CARD_CATEGORIES)), eq(GIFT_CARD_AMOUNT_UNIT));
        assertThat(since.getValue()).isBetween(
            beforeCall.minusMinutes(SPLIT_WINDOW_MINUTES),
            LocalDateTime.now(ZONE_SEOUL).minusMinutes(SPLIT_WINDOW_MINUTES));
    }

    @Test
    void collect_appliesRiskyRepeatedWindowToRiskyCategoryCutoff() {
        LocalDateTime beforeCall = LocalDateTime.now(ZONE_SEOUL);

        collector().collect(WALLET_ID, AMOUNT, merchant("JEWELRY"));

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        then(paymentFdsHistoryMapper).should().countRiskyCategoryPayments(
            eq(WALLET_ID), since.capture(), eq(List.of(RISKY_CATEGORIES)));
        assertThat(since.getValue()).isBetween(
            beforeCall.minusMinutes(RISKY_REPEATED_WINDOW_MINUTES),
            LocalDateTime.now(ZONE_SEOUL).minusMinutes(RISKY_REPEATED_WINDOW_MINUTES));
    }

    @Test
    void collect_appliesLastPaymentWindowToLastCompletedPaymentCutoff() {
        LocalDateTime beforeCall = LocalDateTime.now(ZONE_SEOUL);

        collector().collect(WALLET_ID, AMOUNT, merchant("RESTAURANT"));

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        then(paymentFdsHistoryMapper).should()
            .findLastCompletedPayment(eq(WALLET_ID), since.capture());
        assertThat(since.getValue()).isBetween(
            beforeCall.minusHours(LAST_PAYMENT_WINDOW_HOURS),
            LocalDateTime.now(ZONE_SEOUL).minusHours(LAST_PAYMENT_WINDOW_HOURS));
    }

    // 목록이 비면 IN 절이 성립하지 않으므로 쿼리 없이 0이어야 한다.
    @Test
    void collect_skipsGiftCardQueryWhenCategoriesEmpty() {
        PaymentRuleContextCollectorImpl collector = collector(RISKY_CATEGORIES, new String[0]);

        PaymentRuleContext context = collector.collect(WALLET_ID, AMOUNT, merchant("CVS"));

        assertThat(context.getGiftCardSuspectRecentCount()).isZero();
        then(paymentFdsHistoryMapper).should(never())
            .countGiftCardSuspectPayments(anyLong(), any(LocalDateTime.class), anyList(), anyLong());
    }

    @Test
    void collect_skipsRiskyQueryWhenCategoriesEmpty() {
        PaymentRuleContextCollectorImpl collector = collector(new String[0], GIFT_CARD_CATEGORIES);

        PaymentRuleContext context = collector.collect(WALLET_ID, AMOUNT, merchant("JEWELRY"));

        assertThat(context.getRiskyCategoryRecentCount()).isZero();
        then(paymentFdsHistoryMapper).should(never())
            .countRiskyCategoryPayments(anyLong(), any(LocalDateTime.class), anyList());
    }
}
