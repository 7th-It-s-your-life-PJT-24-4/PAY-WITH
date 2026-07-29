package com.paywith.fds.service;

import static com.paywith.fds.support.RuleContexts.normal;
import static com.paywith.fds.support.RuleContexts.won;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

import com.paywith.fds.domain.DecidedBy;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.domain.RiskRule;
import com.paywith.fds.dto.FdsDecision;
import com.paywith.fds.dto.FdsEvaluationRequest;
import com.paywith.fds.service.rule.RiskRuleCache;
import com.paywith.fds.service.rule.RuleContext;
import com.paywith.fds.support.FdsTestWiring;
import com.paywith.fds.support.RiskRules;
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
@DisplayName("FDS 평가 서비스")
class FdsEvaluationServiceImplTest {

    private static final Long TRANSACTION_ID = 100L;

    @Mock
    private RuleContextCollectorService ruleContextCollectorService;
    @Mock
    private FdsEvaluationResultService fdsEvaluationResultService;
    @Mock
    private RiskRuleCache riskRuleCache;

    private FdsEvaluationServiceImpl service;

    @BeforeEach
    void setUp() {
        List<RiskRule> activeRules = RiskRules.activeRules();
        lenient().when(riskRuleCache.getActiveRules()).thenReturn(activeRules);
        for (RiskRule rule : activeRules) {
            lenient().when(riskRuleCache.findByCode(rule.getRuleCode()))
                .thenReturn(Optional.of(rule));
        }

        service = new FdsEvaluationServiceImpl(
            ruleContextCollectorService,
            fdsEvaluationResultService,
            riskRuleCache,
            FdsTestWiring.grader(),
            FdsTestWiring.evaluators()
        );
    }

    @Nested
    @DisplayName("진입점")
    class Evaluate {

        private FdsEvaluationRequest request() {
            return new FdsEvaluationRequest(TRANSACTION_ID, 1L, 2L, won("30000"), null);
        }

        @Test
        void evaluate_returnsRiskLevelOnly() {
            FdsEvaluationRequest request = request();
            given(ruleContextCollectorService.collect(request)).willReturn(normal().build());

            RiskLevel level = service.evaluate(request);

            assertThat(level).isEqualTo(RiskLevel.SAFE);
        }

        @Test
        void evaluate_delegatesContextCollectingToCollector() {
            FdsEvaluationRequest request = request();
            given(ruleContextCollectorService.collect(request)).willReturn(normal().build());

            service.evaluate(request);

            then(ruleContextCollectorService).should().collect(request);
        }

        @Test
        void evaluate_savesDecisionBeforeReturning() {
            FdsEvaluationRequest request = request();
            given(ruleContextCollectorService.collect(request))
                .willReturn(normal().recipientSendCount(0).amount(won("800000")).build());

            RiskLevel level = service.evaluate(request);

            assertThat(level).isEqualTo(RiskLevel.CAUTION);
            then(fdsEvaluationResultService).should()
                .save(org.mockito.ArgumentMatchers.eq(TRANSACTION_ID),
                    org.mockito.ArgumentMatchers.any(FdsDecision.class));
        }
    }

    @Nested
    @DisplayName("판정")
    class Decide {

        @Test
        void decide_sumsScoresOfTriggeredRulesOnly() {
            // 신규(15) + 고액 L1(10) = 25
            RuleContext context = normal().recipientSendCount(0).amount(won("800000")).build();

            FdsDecision decision = service.decide(context);

            assertThat(decision.getTotalScore()).isEqualTo(25);
            assertThat(decision.getDecidedBy()).isEqualTo(DecidedBy.RULE);
            assertThat(decision.getTriggeredRules()).hasSize(2);
        }

        @Test
        void decide_clampsNegativeTotalToZero() {
            RuleContext context = normal().recipientRegisteredSafe(true).build();

            FdsDecision decision = service.decide(context);

            assertThat(decision.getTotalScore()).isZero();
            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.SAFE);
        }

        @Test
        void decide_ignoresRuleRowsWithoutEvaluator() {
            FdsDecision decision = service.decide(normal().build());

            assertThat(decision.getTotalScore()).isZero();
            assertThat(decision.getTriggeredRules()).isEmpty();
        }

        @Test
        void decide_carriesThresholdSnapshot() {
            FdsDecision decision = service.decide(normal().build());

            assertThat(decision.getCautionThreshold()).isEqualTo(FdsTestWiring.CAUTION_THRESHOLD);
            assertThat(decision.getDangerThreshold()).isEqualTo(FdsTestWiring.DANGER_THRESHOLD);
        }
    }
}
