package com.paywith.fds.service.rule.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.paywith.fds.service.rule.RuleContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RiskRuleEvaluatorTest {

    private static final List<String> MEMO_KEYWORDS = List.of("검찰", "안전계좌", "대포통장");

    /** 기본값 컨텍스트에서 필요한 필드만 바꿔서 만드는 헬퍼 */
    private static RuleContext context(
        BigDecimal amount,
        String memo,
        LocalDateTime requestedAt,
        int recipientSendCount,
        boolean recipientRegisteredSafe,
        int recentTransferCount,
        int recentDistinctRecipientCount
    ) {
        return new RuleContext(
            amount, memo, requestedAt, recipientSendCount, recipientRegisteredSafe,
            recentTransferCount, recentDistinctRecipientCount, MEMO_KEYWORDS
        );
    }

    private static RuleContext defaultContext() {
        return context(
            BigDecimal.valueOf(10000), null,
            LocalDateTime.of(2026, 7, 23, 14, 0),
            5, false, 0, 0
        );
    }

    @Nested
    class HighAmount {

        private final HighAmountRuleEvaluator evaluator = new HighAmountRuleEvaluator(500000L);

        // 경계값: 기준액(50만 원)과 정확히 같으면 발동해야 한다
        @Test
        void triggersWhenAmountIsAtOrAboveThreshold() {
            RuleContext ctx = context(BigDecimal.valueOf(500000), null,
                LocalDateTime.of(2026, 7, 23, 14, 0), 5, false, 0, 0);

            assertThat(evaluator.evaluate(ctx)).isTrue();
        }

        // 경계값: 기준액보다 1원 모자라면 발동하지 않는다
        @Test
        void doesNotTriggerBelowThreshold() {
            RuleContext ctx = context(BigDecimal.valueOf(499999), null,
                LocalDateTime.of(2026, 7, 23, 14, 0), 5, false, 0, 0);

            assertThat(evaluator.evaluate(ctx)).isFalse();
        }
    }

    @Nested
    class NightTime {

        // 자정을 넘는 구간: 22시 ~ 06시
        private final NightTimeRuleEvaluator evaluator = new NightTimeRuleEvaluator(22, 6);

        // 경계값: 심야 시작 시각(22:00) 정각부터 발동한다
        @Test
        void triggersAtStartHour() {
            assertThat(evaluator.evaluate(at(22, 0))).isTrue();
        }

        // 자정을 넘긴 새벽(02:30)도 심야 구간에 포함된다
        @Test
        void triggersAfterMidnight() {
            assertThat(evaluator.evaluate(at(2, 30))).isTrue();
        }

        // 경계값: 심야 종료 시각(06:00) 정각부터는 발동하지 않는다
        @Test
        void doesNotTriggerAtEndHour() {
            assertThat(evaluator.evaluate(at(6, 0))).isFalse();
        }

        // 경계값: 시작 직전(21:59)은 심야가 아니다
        @Test
        void doesNotTriggerJustBeforeStartHour() {
            assertThat(evaluator.evaluate(at(21, 59))).isFalse();
        }

        // 자정을 넘지 않는 설정(01~05시)은 AND 분기로 판정된다
        @Test
        void evaluatesNonWrappingWindowCorrectly() {
            NightTimeRuleEvaluator sameDay = new NightTimeRuleEvaluator(1, 5);

            assertThat(sameDay.evaluate(at(3, 0))).isTrue();
            assertThat(sameDay.evaluate(at(5, 0))).isFalse();
        }

        private RuleContext at(int hour, int minute) {
            return context(BigDecimal.valueOf(10000), null,
                LocalDateTime.of(2026, 7, 23, hour, minute), 5, false, 0, 0);
        }
    }

    @Nested
    class NewRecipient {

        private final NewRecipientRuleEvaluator evaluator = new NewRecipientRuleEvaluator();

        // 송금 이력이 0회인 신규 수취인이면 발동한다
        @Test
        void triggersWhenNoSendHistory() {
            RuleContext ctx = context(BigDecimal.valueOf(10000), null,
                LocalDateTime.of(2026, 7, 23, 14, 0), 0, false, 0, 0);

            assertThat(evaluator.evaluate(ctx)).isTrue();
        }

        // 한 번이라도 보낸 적 있는 수취인이면 발동하지 않는다
        @Test
        void doesNotTriggerWithSendHistory() {
            RuleContext ctx = context(BigDecimal.valueOf(10000), null,
                LocalDateTime.of(2026, 7, 23, 14, 0), 1, false, 0, 0);

            assertThat(evaluator.evaluate(ctx)).isFalse();
        }
    }

    @Nested
    class SafeAccountCheck {

        private final SafeAccountCheckRuleEvaluator evaluator = new SafeAccountCheckRuleEvaluator();

        // 안심계좌로 등록된 수취인이면 발동한다(유일한 감점 룰)
        @Test
        void triggersForSafeRegisteredAccount() {
            RuleContext ctx = context(BigDecimal.valueOf(10000), null,
                LocalDateTime.of(2026, 7, 23, 14, 0), 5, true, 0, 0);

            assertThat(evaluator.evaluate(ctx)).isTrue();
        }

        // 안심계좌 미등록이면 발동하지 않는다
        @Test
        void doesNotTriggerForUnregisteredAccount() {
            assertThat(evaluator.evaluate(defaultContext())).isFalse();
        }
    }

    @Nested
    class Repeated {

        private final RepeatedRuleEvaluator evaluator = new RepeatedRuleEvaluator(3);

        // 경계값: 시간창 내 송금 횟수가 기준(3회)에 정확히 도달하면 발동한다
        @Test
        void triggersWhenRecentTransferCountReachesThreshold() {
            RuleContext ctx = context(BigDecimal.valueOf(10000), null,
                LocalDateTime.of(2026, 7, 23, 14, 0), 5, false, 3, 0);

            assertThat(evaluator.evaluate(ctx)).isTrue();
        }

        // 경계값: 기준보다 1회 모자라면 발동하지 않는다
        @Test
        void doesNotTriggerBelowThreshold() {
            RuleContext ctx = context(BigDecimal.valueOf(10000), null,
                LocalDateTime.of(2026, 7, 23, 14, 0), 5, false, 2, 0);

            assertThat(evaluator.evaluate(ctx)).isFalse();
        }
    }

    @Nested
    class DivisionTransfer {

        private final DivisionTransferRuleEvaluator evaluator = new DivisionTransferRuleEvaluator(3);

        // 경계값: 시간창 내 서로 다른 수취인 수가 기준(3곳)에 정확히 도달하면 발동한다
        @Test
        void triggersWhenDistinctRecipientCountReachesThreshold() {
            RuleContext ctx = context(BigDecimal.valueOf(10000), null,
                LocalDateTime.of(2026, 7, 23, 14, 0), 5, false, 0, 3);

            assertThat(evaluator.evaluate(ctx)).isTrue();
        }

        // 경계값: 기준보다 1곳 모자라면 발동하지 않는다
        @Test
        void doesNotTriggerBelowThreshold() {
            RuleContext ctx = context(BigDecimal.valueOf(10000), null,
                LocalDateTime.of(2026, 7, 23, 14, 0), 5, false, 0, 2);

            assertThat(evaluator.evaluate(ctx)).isFalse();
        }
    }

    @Nested
    class SuspiciousMemo {

        private final SuspiciousMemoRuleEvaluator evaluator = new SuspiciousMemoRuleEvaluator();

        // 메모에 위험 키워드가 포함되면 발동한다
        @Test
        void triggersWhenMemoContainsKeyword() {
            RuleContext ctx = context(BigDecimal.valueOf(10000), "안전계좌로 옮겨주세요",
                LocalDateTime.of(2026, 7, 23, 14, 0), 5, false, 0, 0);

            assertThat(evaluator.evaluate(ctx)).isTrue();
        }

        // 키워드가 없는 일반 메모는 발동하지 않는다
        @Test
        void doesNotTriggerForNormalMemo() {
            RuleContext ctx = context(BigDecimal.valueOf(10000), "생일 축하해",
                LocalDateTime.of(2026, 7, 23, 14, 0), 5, false, 0, 0);

            assertThat(evaluator.evaluate(ctx)).isFalse();
        }

        // 메모가 null이면 발동하지 않는다(NPE 방어 확인)
        @Test
        void doesNotTriggerWhenMemoIsNull() {
            assertThat(evaluator.evaluate(defaultContext())).isFalse();
        }

        // 메모가 공백뿐이면 발동하지 않는다
        @Test
        void doesNotTriggerWhenMemoIsBlank() {
            RuleContext ctx = context(BigDecimal.valueOf(10000), "   ",
                LocalDateTime.of(2026, 7, 23, 14, 0), 5, false, 0, 0);

            assertThat(evaluator.evaluate(ctx)).isFalse();
        }
    }
}
