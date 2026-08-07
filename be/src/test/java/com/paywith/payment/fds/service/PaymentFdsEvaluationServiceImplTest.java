package com.paywith.payment.fds.service;

import static com.paywith.payment.fds.support.PaymentRuleContexts.BUSAN_LAT;
import static com.paywith.payment.fds.support.PaymentRuleContexts.BUSAN_LNG;
import static com.paywith.payment.fds.support.PaymentRuleContexts.at;
import static com.paywith.payment.fds.support.PaymentRuleContexts.lastPayment;
import static com.paywith.payment.fds.support.PaymentRuleContexts.normal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import com.paywith.fds.domain.DecidedBy;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.domain.RiskRule;
import com.paywith.fds.dto.FdsDecision;
import com.paywith.fds.service.rule.RiskRuleCache;
import com.paywith.merchant.domain.Merchant;
import com.paywith.payment.fds.rule.PaymentRuleContext;
import com.paywith.payment.fds.rule.impl.PaymentImpossibleTravelEvaluator;
import com.paywith.payment.fds.support.PaymentFdsTestWiring;
import com.paywith.payment.fds.support.PaymentRiskRules;
import com.paywith.fds.support.RiskRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("결제 FDS 평가 서비스")
class PaymentFdsEvaluationServiceImplTest {

    @Mock
    private PaymentRuleContextCollector paymentRuleContextCollector;
    @Mock
    private RiskRuleCache riskRuleCache;

    private PaymentFdsEvaluationServiceImpl service;

    @BeforeEach
    void setUp() {
        // 실제 기동 상태처럼 송금 룰과 결제 룰이 한 카탈로그에 섞여 있다 — 결제 서비스는
        // 자기 평가기 맵에 있는 PAY_* 코드만 집어야 한다.
        List<RiskRule> activeRules = new ArrayList<>(RiskRules.activeRules());
        activeRules.addAll(PaymentRiskRules.activeRules());
        org.mockito.Mockito.lenient().when(riskRuleCache.getActiveRules()).thenReturn(activeRules);
        for (RiskRule rule : activeRules) {
            org.mockito.Mockito.lenient().when(riskRuleCache.findByCode(rule.getRuleCode()))
                .thenReturn(Optional.of(rule));
        }

        service = new PaymentFdsEvaluationServiceImpl(
            paymentRuleContextCollector,
            PaymentFdsTestWiring.travelEvaluator(),
            riskRuleCache,
            PaymentFdsTestWiring.grader(),
            PaymentFdsTestWiring.evaluators()
        );
    }

    @Nested
    @DisplayName("진입점")
    class Evaluate {

        private Merchant merchant() {
            Merchant merchant = new Merchant();
            merchant.setMerchantId(1L);
            return merchant;
        }

        @Test
        void evaluate_delegatesContextCollectingToCollector() {
            Merchant merchant = merchant();
            given(paymentRuleContextCollector.collect(1L, 31_000L, merchant))
                .willReturn(normal().build());

            FdsDecision decision = service.evaluate(1L, 31_000L, merchant);

            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.SAFE);
        }

        /** 판정 불능은 fail-open(D4) — 예외를 삼키고 SAFE 로 후퇴해 결제를 막지 않는다. */
        @Test
        void evaluate_failsOpenWhenCollectorThrows() {
            given(paymentRuleContextCollector.collect(anyLong(), anyLong(), any()))
                .willThrow(new RuntimeException("DB 연결 끊김"));

            assertThatCode(() -> {
                FdsDecision decision = service.evaluate(1L, 31_000L, merchant());
                assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.SAFE);
                assertThat(decision.getTriggeredRules()).isEmpty();
                assertThat(decision.getTotalScore()).isZero();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("단축평가")
    class Shortcut {

        /** 서울 결제 5분 전 부산 결제 — 점수 룰이 여럿 걸리는 컨텍스트여도 단축평가가 우선한다. */
        private PaymentRuleContext.PaymentRuleContextBuilder impossibleTravelContext() {
            return normal()
                .merchantCategoryCode("JEWELRY")
                .amount(500_000L)
                .lastCompletedPayment(
                    lastPayment(BUSAN_LAT, BUSAN_LNG, at(14).minusMinutes(5)));
        }

        /** 확정 위험이라 룰 행의 만점이 그대로 총점이 된다 — 차단인데 0점으로 보이던 문제를 막는다. */
        @Test
        void decidesDangerWithFullScoreBeforeScoringRules() {
            FdsDecision decision = service.decide(impossibleTravelContext().build());

            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.DANGER);
            assertThat(decision.getDecidedBy()).isEqualTo(DecidedBy.BLACKLIST);
            assertThat(decision.getTotalScore()).isEqualTo(PaymentRiskRules.PREFILTER_SCORE);
            assertThat(decision.getTriggeredRules()).hasSize(1);
            assertThat(decision.getTriggeredRules().get(0).getScore())
                .isEqualTo(PaymentRiskRules.PREFILTER_SCORE);
        }

        /** 활성 행이 없으면(시드 누락·비활성) 단축평가로 차단하지 않는다 — 송금 블랙리스트와 동일. */
        @Test
        void fallsThroughToScoringWhenRuleRowIsMissing() {
            given(riskRuleCache.findByCode("PAY_IMPOSSIBLE_TRAVEL")).willReturn(Optional.empty());

            FdsDecision decision = service.decide(impossibleTravelContext().build());

            // 위험업종(50) + 고액 L3(70) = 120 → 상한 100 — 점수 경로로 넘어가 DANGER
            assertThat(decision.getDecidedBy()).isEqualTo(DecidedBy.RULE);
            assertThat(decision.getTotalScore()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("점수 조합 (설계서 §4-2 확정 산수)")
    class ScoreCombinations {

        @Test
        void riskyCategoryAloneIsExactlyCaution() {
            FdsDecision decision = service.decide(
                normal().merchantCategoryCode("JEWELRY").amount(90_000L).build());

            assertThat(decision.getTotalScore()).isEqualTo(50);
            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.CAUTION);
        }

        /** 50 + 70 = 120 이라 상한에 걸린다. 잘려도 위험 문턱(100) 위라 등급은 그대로다. */
        @Test
        void riskyCategoryWithHighAmountL3IsDanger() {
            FdsDecision decision = service.decide(
                normal().merchantCategoryCode("JEWELRY").amount(500_000L).build());

            assertThat(decision.getTotalScore()).isEqualTo(100);
            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.DANGER);
        }

        @Test
        void giftCardAmountAloneIsSafeRecordOnly() {
            FdsDecision decision = service.decide(
                normal().merchantCategoryCode("CVS").amount(50_000L).build());

            assertThat(decision.getTotalScore()).isEqualTo(20);
            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.SAFE);
        }

        @Test
        void splitPaymentAloneIsCaution() {
            FdsDecision decision = service.decide(
                normal().merchantCategoryCode("CVS").amount(10_000L)
                    .giftCardSuspectRecentCount(2).build());

            assertThat(decision.getTotalScore()).isEqualTo(80);
            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.CAUTION);
        }

        /** 10만 원대 분할부터 차단된다 — 분할(80) + L1(20) = 정확히 위험 문턱. */
        @Test
        void splitPaymentWithL1IsDanger() {
            FdsDecision decision = service.decide(
                normal().merchantCategoryCode("CVS").amount(100_000L)
                    .giftCardSuspectRecentCount(2).build());

            assertThat(decision.getTotalScore()).isEqualTo(100);
            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.DANGER);
        }

        /** 분할(80) + 심야(28) = 108 이라 상한에 걸린다. */
        @Test
        void splitPaymentAtDeepNightIsDanger() {
            FdsDecision decision = service.decide(
                normal().merchantCategoryCode("CVS").amount(10_000L)
                    .giftCardSuspectRecentCount(2).requestedAt(at(2)).build());

            assertThat(decision.getTotalScore()).isEqualTo(100);
            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.DANGER);
        }

        /** 일괄 구매(L3+GIFT=90 알림)가 분할(100 차단)보다 유리해야 회피 인센티브가 없다. */
        @Test
        void bulkGiftCardPurchaseIsCautionNotDanger() {
            FdsDecision decision = service.decide(
                normal().merchantCategoryCode("CVS").amount(500_000L).build());

            assertThat(decision.getTotalScore()).isEqualTo(90);
            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.CAUTION);
        }

        @Test
        void deepNightWithHighAmountL3StaysCaution() {
            FdsDecision decision = service.decide(
                normal().amount(500_000L).requestedAt(at(2)).build());

            assertThat(decision.getTotalScore()).isEqualTo(98);
            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.CAUTION);
        }
    }

    @Nested
    @DisplayName("판정")
    class Decide {

        /** 카탈로그에 섞인 송금 룰 행은 결제 평가기가 없어 무시된다(대칭 무해 구조). */
        @Test
        void decide_ignoresTransferRuleRowsInSharedCatalog() {
            FdsDecision decision = service.decide(normal().build());

            assertThat(decision.getTotalScore()).isZero();
            assertThat(decision.getTriggeredRules()).isEmpty();
            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.SAFE);
        }

        @Test
        void decide_carriesThresholdSnapshot() {
            FdsDecision decision = service.decide(normal().build());

            assertThat(decision.getCautionThreshold())
                .isEqualTo(PaymentFdsTestWiring.CAUTION_THRESHOLD);
            assertThat(decision.getDangerThreshold())
                .isEqualTo(PaymentFdsTestWiring.DANGER_THRESHOLD);
        }
    }
}
