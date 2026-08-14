package com.paywith.payment.fds.rule.impl;

import static com.paywith.payment.fds.support.PaymentRuleContexts.at;
import static com.paywith.payment.fds.support.PaymentRuleContexts.normal;
import static org.assertj.core.api.Assertions.assertThat;

import com.paywith.payment.fds.rule.PaymentRiskRuleEvaluator;
import com.paywith.payment.fds.rule.PaymentRuleContext;
import com.paywith.payment.fds.support.PaymentFdsTestWiring;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** 구간 룰은 경계값과 계열 내 배타성을 함께 본다(송금 평가기 테스트와 같은 방식). */
@DisplayName("결제 FDS 룰 평가기")
class PaymentRiskRuleEvaluatorTest {

    private final PaymentHighAmountL1RuleEvaluator amountL1 =
        new PaymentHighAmountL1RuleEvaluator(PaymentFdsTestWiring.L1, PaymentFdsTestWiring.L2);
    private final PaymentHighAmountL2RuleEvaluator amountL2 =
        new PaymentHighAmountL2RuleEvaluator(PaymentFdsTestWiring.L2, PaymentFdsTestWiring.L3);
    private final PaymentHighAmountL3RuleEvaluator amountL3 =
        new PaymentHighAmountL3RuleEvaluator(PaymentFdsTestWiring.L3);
    private final PaymentNightLateRuleEvaluator nightLate =
        new PaymentNightLateRuleEvaluator(PaymentFdsTestWiring.LATE_START_HOUR);
    private final PaymentNightDeepRuleEvaluator nightDeep =
        new PaymentNightDeepRuleEvaluator(PaymentFdsTestWiring.DEEP_END_HOUR);
    private final PaymentRiskyCategoryRuleEvaluator risky = new PaymentRiskyCategoryRuleEvaluator();
    private final PaymentRiskyRepeatedRuleEvaluator riskyRepeated =
        new PaymentRiskyRepeatedRuleEvaluator(PaymentFdsTestWiring.RISKY_REPEATED_COUNT_THRESHOLD);
    private final PaymentGiftCardAmountRuleEvaluator giftCard =
        new PaymentGiftCardAmountRuleEvaluator(PaymentFdsTestWiring.SPLIT_COUNT_THRESHOLD);
    private final PaymentSplitPaymentRuleEvaluator split =
        new PaymentSplitPaymentRuleEvaluator(PaymentFdsTestWiring.SPLIT_COUNT_THRESHOLD);

    /** 계열 안에서 발동한 룰 코드를 모은다. 배타성 검증용. */
    private List<String> triggeredIn(PaymentRuleContext context, PaymentRiskRuleEvaluator... family) {
        return java.util.Arrays.stream(family)
            .filter(evaluator -> evaluator.evaluate(context))
            .map(PaymentRiskRuleEvaluator::getRuleCode)
            .collect(java.util.stream.Collectors.toList());
    }

    @Nested
    @DisplayName("고액 구간")
    class HighAmount {

        @Test
        void doesNotTriggerBelowL1() {
            PaymentRuleContext context = normal().amount(99_999L).build();
            assertThat(triggeredIn(context, amountL1, amountL2, amountL3)).isEmpty();
        }

        @Test
        void triggersL1AtLowerBoundary() {
            PaymentRuleContext context = normal().amount(100_000L).build();
            assertThat(triggeredIn(context, amountL1, amountL2, amountL3))
                .containsExactly("PAY_HIGH_AMOUNT_L1");
        }

        @Test
        void triggersL2AtLowerBoundary() {
            PaymentRuleContext context = normal().amount(300_000L).build();
            assertThat(triggeredIn(context, amountL1, amountL2, amountL3))
                .containsExactly("PAY_HIGH_AMOUNT_L2");
        }

        /** 정확히 L3(50만) — 개발 시드 잔액 전액 결제 시연이 이 경계에 걸린다. */
        @Test
        void triggersL3AtLowerBoundary() {
            PaymentRuleContext context = normal().amount(500_000L).build();
            assertThat(triggeredIn(context, amountL1, amountL2, amountL3))
                .containsExactly("PAY_HIGH_AMOUNT_L3");
        }

        @Test
        void triggersL3WithNoUpperBound() {
            PaymentRuleContext context = normal().amount(999_999_999L).build();
            assertThat(triggeredIn(context, amountL1, amountL2, amountL3))
                .containsExactly("PAY_HIGH_AMOUNT_L3");
        }

        @Test
        void triggersAtMostOneTierPerAmount() {
            for (long amount : List.of(0L, 99_999L, 100_000L, 299_999L, 300_000L,
                499_999L, 500_000L, 10_000_000L)) {
                PaymentRuleContext context = normal().amount(amount).build();
                assertThat(triggeredIn(context, amountL1, amountL2, amountL3))
                    .as("금액 %d 에서 발동한 구간 수", amount)
                    .hasSizeLessThanOrEqualTo(1);
            }
        }
    }

    @Nested
    @DisplayName("심야 구간")
    class NightTime {

        @Test
        void triggersDeepJustAfterMidnight() {
            PaymentRuleContext context = normal().requestedAt(at(0)).build();
            assertThat(triggeredIn(context, nightLate, nightDeep))
                .containsExactly("PAY_NIGHT_DEEP");
        }

        @Test
        void triggersDeepAtLastNightHour() {
            PaymentRuleContext context = normal().requestedAt(at(5)).build();
            assertThat(triggeredIn(context, nightLate, nightDeep))
                .containsExactly("PAY_NIGHT_DEEP");
        }

        @Test
        void doesNotTriggerAtDeepEndHour() {
            PaymentRuleContext context = normal().requestedAt(at(6)).build();
            assertThat(triggeredIn(context, nightLate, nightDeep)).isEmpty();
        }

        @Test
        void triggersLateAtStartHour() {
            PaymentRuleContext context = normal().requestedAt(at(22)).build();
            assertThat(triggeredIn(context, nightLate, nightDeep))
                .containsExactly("PAY_NIGHT_LATE");
        }

        @Test
        void triggersAtMostOneTierPerHour() {
            for (int hour = 0; hour < 24; hour++) {
                PaymentRuleContext context = normal().requestedAt(at(hour)).build();
                assertThat(triggeredIn(context, nightLate, nightDeep))
                    .as("%d시에 발동한 구간 수", hour)
                    .hasSizeLessThanOrEqualTo(1);
            }
        }
    }

    @Nested
    @DisplayName("승인 대기 중 결제")
    class PendingApproval {

        private final PaymentPendingApprovalRuleEvaluator evaluator =
            new PaymentPendingApprovalRuleEvaluator();

        @Test
        void triggersWhenApprovalIsStillPending() {
            assertThat(evaluator.evaluate(normal().pendingApprovalExists(true).build())).isTrue();
        }

        @Test
        void doesNotTriggerWhenNoPendingApproval() {
            assertThat(evaluator.evaluate(normal().build())).isFalse();
        }
    }

    @Nested
    @DisplayName("위험 업종")
    class RiskyCategory {

        @Test
        void triggersForRiskyCategory() {
            assertThat(risky.evaluate(normal().merchantCategoryCode("JEWELRY").build())).isTrue();
        }

        /** 목록 두 번째 이후 코드도 발동해야 한다 — 복수 코드 파싱 확인. */
        @Test
        void triggersForEveryCodeInList() {
            for (String code : List.of("JEWELRY", "ELECTRONICS", "GIFT_CARD", "LUXURY")) {
                assertThat(risky.evaluate(normal().merchantCategoryCode(code).build()))
                    .as("위험 업종 %s", code)
                    .isTrue();
            }
        }

        @Test
        void doesNotTriggerForNeutralCategory() {
            assertThat(risky.evaluate(normal().merchantCategoryCode("RESTAURANT").build())).isFalse();
        }

        @Test
        void doesNotTriggerForGiftCardCategory() {
            assertThat(risky.evaluate(normal().merchantCategoryCode("CVS").build())).isFalse();
        }

        @Test
        void skipsWhenCategoryCodeIsNull() {
            assertThat(risky.evaluate(normal().merchantCategoryCode(null).build())).isFalse();
        }
    }

    @Nested
    @DisplayName("위험 업종 반복")
    class RiskyRepeated {

        @Test
        void triggersAtPastCountThreshold() {
            PaymentRuleContext context = normal()
                .merchantCategoryCode("JEWELRY").riskyCategoryRecentCount(2).build();
            assertThat(riskyRepeated.evaluate(context)).isTrue();
        }

        @Test
        void doesNotTriggerBelowPastCountThreshold() {
            PaymentRuleContext context = normal()
                .merchantCategoryCode("JEWELRY").riskyCategoryRecentCount(1).build();
            assertThat(riskyRepeated.evaluate(context)).isFalse();
        }

        /** 현재 결제가 위험 업종이 아니면 과거가 아무리 많아도 반복이 아니다. */
        @Test
        void doesNotTriggerWhenCurrentPaymentIsNotRiskyCategory() {
            PaymentRuleContext context = normal()
                .merchantCategoryCode("RESTAURANT").riskyCategoryRecentCount(5).build();
            assertThat(riskyRepeated.evaluate(context)).isFalse();
        }

        @Test
        void skipsWhenCategoryCodeIsNull() {
            PaymentRuleContext context = normal()
                .merchantCategoryCode(null).riskyCategoryRecentCount(5).build();
            assertThat(riskyRepeated.evaluate(context)).isFalse();
        }
    }

    @Nested
    @DisplayName("상품권 의심 금액")
    class GiftCardAmount {

        @Test
        void triggersForUnitMultipleAtGiftCardMerchant() {
            PaymentRuleContext context =
                normal().merchantCategoryCode("CVS").amount(50_000L).build();
            assertThat(giftCard.evaluate(context)).isTrue();
        }

        /** 5,000 배수 경계 — 4,999/5,001은 비배수, 5,000/10,000은 배수. */
        @Test
        void triggersOnlyForExactUnitMultiples() {
            assertThat(giftCard.evaluate(normal().merchantCategoryCode("CVS").amount(4_999L).build())).isFalse();
            assertThat(giftCard.evaluate(normal().merchantCategoryCode("CVS").amount(5_000L).build())).isTrue();
            assertThat(giftCard.evaluate(normal().merchantCategoryCode("CVS").amount(5_001L).build())).isFalse();
            assertThat(giftCard.evaluate(normal().merchantCategoryCode("CVS").amount(10_000L).build())).isTrue();
        }

        @Test
        void doesNotTriggerAtNeutralMerchantEvenForUnitMultiple() {
            PaymentRuleContext context =
                normal().merchantCategoryCode("RESTAURANT").amount(50_000L).build();
            assertThat(giftCard.evaluate(context)).isFalse();
        }

        @Test
        void skipsWhenCategoryCodeIsNull() {
            PaymentRuleContext context = normal().merchantCategoryCode(null).amount(50_000L).build();
            assertThat(giftCard.evaluate(context)).isFalse();
        }

        /** 목록 2종이 겹치는 설정 오류 시 위험 업종 판정이 우선한다(서로소 규칙의 방어 동작). */
        @Test
        void yieldsToRiskyCategoryWhenListsOverlap() {
            PaymentRuleContext context = normal()
                .merchantCategoryCode("MART")
                .amount(50_000L)
                .riskyCategories(List.of("MART"))
                .build();
            assertThat(giftCard.evaluate(context)).isFalse();
            assertThat(risky.evaluate(context)).isTrue();
        }
    }

    @Nested
    @DisplayName("분할 결제")
    class SplitPayment {

        @Test
        void triggersAtPastCountThreshold() {
            PaymentRuleContext context = normal()
                .merchantCategoryCode("CVS").amount(10_000L).giftCardSuspectRecentCount(2).build();
            assertThat(split.evaluate(context)).isTrue();
        }

        @Test
        void doesNotTriggerBelowPastCountThreshold() {
            PaymentRuleContext context = normal()
                .merchantCategoryCode("CVS").amount(10_000L).giftCardSuspectRecentCount(1).build();
            assertThat(split.evaluate(context)).isFalse();
        }

        /** 현재 결제가 상품권 의심이 아니면 과거가 아무리 많아도 분할이 아니다. */
        @Test
        void doesNotTriggerWhenCurrentPaymentIsNotGiftCardSuspect() {
            PaymentRuleContext context = normal()
                .merchantCategoryCode("RESTAURANT").amount(10_000L).giftCardSuspectRecentCount(5).build();
            assertThat(split.evaluate(context)).isFalse();
        }

        /** GIFT 와 SPLIT 은 같은 계열 — 어떤 조합에서도 한 행만 발동한다. */
        @Test
        void triggersAtMostOneOfGiftCardFamily() {
            for (int pastCount : List.of(0, 1, 2, 5)) {
                PaymentRuleContext context = normal()
                    .merchantCategoryCode("CVS").amount(10_000L)
                    .giftCardSuspectRecentCount(pastCount).build();
                assertThat(triggeredIn(context, giftCard, split))
                    .as("과거 의심 %d건에서 발동한 계열 룰 수", pastCount)
                    .hasSize(1);
            }
        }

        /** 반복 조건까지 충족하면 SPLIT 만 발동한다(상위 티어 우선). */
        @Test
        void splitReplacesGiftCardAtThreshold() {
            PaymentRuleContext context = normal()
                .merchantCategoryCode("CVS").amount(10_000L).giftCardSuspectRecentCount(2).build();
            assertThat(triggeredIn(context, giftCard, split))
                .containsExactly("PAY_SPLIT_PAYMENT");
        }
    }
}
