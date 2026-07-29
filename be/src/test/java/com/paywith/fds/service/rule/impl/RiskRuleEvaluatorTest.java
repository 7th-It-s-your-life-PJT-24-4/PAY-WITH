package com.paywith.fds.service.rule.impl;

import static com.paywith.fds.support.RuleContexts.at;
import static com.paywith.fds.support.RuleContexts.normal;
import static com.paywith.fds.support.RuleContexts.won;
import static org.assertj.core.api.Assertions.assertThat;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** 구간 룰은 경계값과 계열 내 배타성을 함께 본다. */
@DisplayName("송금 FDS 룰 평가기")
class RiskRuleEvaluatorTest {

    // application-local.properties 기본값
    private static final long L1 = 500_000L;
    private static final long L2 = 1_500_000L;
    private static final long L3 = 3_000_000L;
    private static final int LATE_START_HOUR = 22;
    private static final int DEEP_END_HOUR = 6;

    private final HighAmountL1RuleEvaluator amountL1 = new HighAmountL1RuleEvaluator(L1, L2);
    private final HighAmountL2RuleEvaluator amountL2 = new HighAmountL2RuleEvaluator(L2, L3);
    private final HighAmountL3RuleEvaluator amountL3 = new HighAmountL3RuleEvaluator(L3);
    private final NightTimeLateRuleEvaluator nightLate = new NightTimeLateRuleEvaluator(LATE_START_HOUR);
    private final NightTimeDeepRuleEvaluator nightDeep = new NightTimeDeepRuleEvaluator(DEEP_END_HOUR);
    private final SuspiciousMemoRuleEvaluator memo = new SuspiciousMemoRuleEvaluator();

    /** 계열 안에서 발동한 룰 코드를 모은다. 배타성 검증용. */
    private List<String> triggeredIn(RuleContext context, RiskRuleEvaluator... family) {
        return java.util.Arrays.stream(family)
            .filter(evaluator -> evaluator.evaluate(context))
            .map(RiskRuleEvaluator::getRuleCode)
            .collect(java.util.stream.Collectors.toList());
    }

    @Nested
    @DisplayName("고액 구간")
    class HighAmount {

        @Test
        void doesNotTriggerBelowL1() {
            RuleContext context = normal().amount(won("499999")).build();
            assertThat(triggeredIn(context, amountL1, amountL2, amountL3)).isEmpty();
        }

        @Test
        void triggersL1AtLowerBoundary() {
            RuleContext context = normal().amount(won("500000")).build();
            assertThat(triggeredIn(context, amountL1, amountL2, amountL3))
                .containsExactly("HIGH_AMOUNT_L1");
        }

        @Test
        void triggersL2AtLowerBoundary() {
            RuleContext context = normal().amount(won("1500000")).build();
            assertThat(triggeredIn(context, amountL1, amountL2, amountL3))
                .containsExactly("HIGH_AMOUNT_L2");
        }

        @Test
        void triggersL3AtLowerBoundary() {
            RuleContext context = normal().amount(won("3000000")).build();
            assertThat(triggeredIn(context, amountL1, amountL2, amountL3))
                .containsExactly("HIGH_AMOUNT_L3");
        }

        @Test
        void triggersL3WithNoUpperBound() {
            RuleContext context = normal().amount(won("999999999")).build();
            assertThat(triggeredIn(context, amountL1, amountL2, amountL3))
                .containsExactly("HIGH_AMOUNT_L3");
        }

        @Test
        void triggersAtMostOneTierPerAmount() {
            for (String amount : List.of("0", "499999", "500000", "1499999", "1500000",
                "2999999", "3000000", "10000000")) {
                RuleContext context = normal().amount(won(amount)).build();
                assertThat(triggeredIn(context, amountL1, amountL2, amountL3))
                    .as("금액 %s 에서 발동한 구간 수", amount)
                    .hasSizeLessThanOrEqualTo(1);
            }
        }
    }

    @Nested
    @DisplayName("심야 구간")
    class NightTime {

        @Test
        void triggersDeepJustAfterMidnight() {
            RuleContext context = normal().requestedAt(at(0)).build();
            assertThat(triggeredIn(context, nightLate, nightDeep))
                .containsExactly("NIGHT_TIME_DEEP");
        }

        @Test
        void triggersDeepAtLastNightHour() {
            RuleContext context = normal().requestedAt(at(5)).build();
            assertThat(triggeredIn(context, nightLate, nightDeep))
                .containsExactly("NIGHT_TIME_DEEP");
        }

        @Test
        void doesNotTriggerAtDeepEndHour() {
            RuleContext context = normal().requestedAt(at(6)).build();
            assertThat(triggeredIn(context, nightLate, nightDeep)).isEmpty();
        }

        @Test
        void doesNotTriggerDuringDaytime() {
            RuleContext context = normal().requestedAt(at(14)).build();
            assertThat(triggeredIn(context, nightLate, nightDeep)).isEmpty();
        }

        @Test
        void triggersLateAtStartHour() {
            RuleContext context = normal().requestedAt(at(22)).build();
            assertThat(triggeredIn(context, nightLate, nightDeep))
                .containsExactly("NIGHT_TIME_LATE");
        }

        @Test
        void triggersAtMostOneTierPerHour() {
            for (int hour = 0; hour < 24; hour++) {
                RuleContext context = normal().requestedAt(at(hour)).build();
                assertThat(triggeredIn(context, nightLate, nightDeep))
                    .as("%d시에 발동한 구간 수", hour)
                    .hasSizeLessThanOrEqualTo(1);
            }
        }
    }

    @Nested
    @DisplayName("의심 메모")
    class SuspiciousMemo {

        @Test
        void doesNotTriggerWhenMemoIsNull() {
            assertThat(memo.evaluate(normal().memo(null).build())).isFalse();
        }

        @Test
        void doesNotTriggerWhenMemoIsBlank() {
            assertThat(memo.evaluate(normal().memo("   ").build())).isFalse();
        }

        @Test
        void doesNotTriggerForOrdinaryMemo() {
            assertThat(memo.evaluate(normal().memo("생일 축하해").build())).isFalse();
        }

        @Test
        void triggersForKeywordAnywhereInMemo() {
            assertThat(memo.evaluate(normal().memo("검찰 수사 협조 요청").build())).isTrue();
        }

        @Test
        void triggersForAnyKeywordInList() {
            assertThat(memo.evaluate(normal().memo("대출 상환금").build())).isTrue();
        }

        @Test
        void triggersOnceRegardlessOfKeywordCount() {
            assertThat(memo.evaluate(normal().memo("검찰이 요구한 대출 상환").build())).isTrue();
        }
    }

    @Nested
    @DisplayName("신규 수취인")
    class NewRecipient {

        private final NewRecipientRuleEvaluator evaluator = new NewRecipientRuleEvaluator();

        @Test
        void triggersWhenSendCountIsZero() {
            assertThat(evaluator.evaluate(normal().recipientSendCount(0).build())).isTrue();
        }

        @Test
        void doesNotTriggerWhenRecipientHasHistory() {
            assertThat(evaluator.evaluate(normal().recipientSendCount(1).build())).isFalse();
        }
    }

    @Nested
    @DisplayName("반복 송금")
    class Repeated {

        private final RepeatedRuleEvaluator evaluator = new RepeatedRuleEvaluator(3);

        @Test
        void triggersAtCountThreshold() {
            assertThat(evaluator.evaluate(normal().recentTransferCount(3).build())).isTrue();
        }

        @Test
        void doesNotTriggerBelowCountThreshold() {
            assertThat(evaluator.evaluate(normal().recentTransferCount(2).build())).isFalse();
        }
    }

    @Nested
    @DisplayName("분할 송금")
    class DivisionTransfer {

        private final DivisionTransferRuleEvaluator evaluator = new DivisionTransferRuleEvaluator(3);

        @Test
        void triggersAtAccountThreshold() {
            assertThat(evaluator.evaluate(normal().recentDistinctRecipientCount(3).build())).isTrue();
        }

        @Test
        void doesNotTriggerBelowAccountThreshold() {
            assertThat(evaluator.evaluate(normal().recentDistinctRecipientCount(2).build())).isFalse();
        }
    }

    @Nested
    @DisplayName("승인 대기 중 추가 송금")
    class PendingApproval {

        private final PendingApprovalRuleEvaluator evaluator = new PendingApprovalRuleEvaluator();

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
    @DisplayName("안전계좌 감점")
    class SafeAccountCheck {

        private final SafeAccountCheckRuleEvaluator evaluator = new SafeAccountCheckRuleEvaluator();

        @Test
        void triggersForSeniorRegisteredAccount() {
            assertThat(evaluator.evaluate(normal().recipientRegisteredSafe(true).build())).isTrue();
        }

        @Test
        void doesNotTriggerForUnregisteredAccount() {
            assertThat(evaluator.evaluate(normal().build())).isFalse();
        }
    }
}
