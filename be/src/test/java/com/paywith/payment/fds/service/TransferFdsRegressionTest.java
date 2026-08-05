package com.paywith.payment.fds.service;

import static com.paywith.fds.support.RuleContexts.normal;
import static com.paywith.fds.support.RuleContexts.won;
import static org.assertj.core.api.Assertions.assertThat;

import com.paywith.fds.domain.RiskRule;
import com.paywith.fds.dto.FdsDecision;
import com.paywith.fds.service.FdsEvaluationServiceImpl;
import com.paywith.fds.service.rule.RiskRuleCache;
import com.paywith.fds.service.rule.RuleContext;
import com.paywith.fds.support.FdsTestWiring;
import com.paywith.fds.support.RiskRules;
import com.paywith.payment.fds.support.PaymentRiskRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 송금 회귀 스모크 — risk_rules 에 PAY_* 행을 추가해도 송금 판정은 변하지 않아야 한다.
 * 송금 서비스의 룰 순회가 평가기 빈 없는 rule_code 를 건너뛰는 동작(설계서 §2 핵심 발견)이
 * 카탈로그 공유의 전제라, 이 테스트가 깨지면 공유 구조 자체를 재검토해야 한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("송금 FDS 회귀 (PAY_* 카탈로그 공유)")
class TransferFdsRegressionTest {

    @Mock
    private RiskRuleCache transferOnlyCache;
    @Mock
    private RiskRuleCache sharedCache;

    private FdsEvaluationServiceImpl transferOnlyService;
    private FdsEvaluationServiceImpl sharedCatalogService;

    @BeforeEach
    void setUp() {
        List<RiskRule> transferRules = RiskRules.activeRules();
        List<RiskRule> sharedRules = new ArrayList<>(RiskRules.activeRules());
        sharedRules.addAll(PaymentRiskRules.activeRules());

        stub(transferOnlyCache, transferRules);
        stub(sharedCache, sharedRules);

        transferOnlyService = transferService(transferOnlyCache);
        sharedCatalogService = transferService(sharedCache);
    }

    private void stub(RiskRuleCache cache, List<RiskRule> rules) {
        org.mockito.Mockito.lenient().when(cache.getActiveRules()).thenReturn(rules);
        for (RiskRule rule : rules) {
            org.mockito.Mockito.lenient().when(cache.findByCode(rule.getRuleCode()))
                .thenReturn(Optional.of(rule));
        }
    }

    private FdsEvaluationServiceImpl transferService(RiskRuleCache cache) {
        return new FdsEvaluationServiceImpl(
            null, null, cache, FdsTestWiring.grader(), FdsTestWiring.evaluators());
    }

    @Test
    void transferDecisionsAreUnchangedByPaymentRuleRows() {
        List<RuleContext> contexts = List.of(
            normal().build(),                                                    // SAFE
            normal().recipientSendCount(0).amount(won("800000")).build(),        // CAUTION 25
            normal().recipientSendCount(0).amount(won("3000000")).build(),       // DANGER 50
            normal().recipientRejectedBefore(true).build()                       // 블랙리스트 DANGER
        );

        for (RuleContext context : contexts) {
            FdsDecision before = transferOnlyService.decide(context);
            FdsDecision after = sharedCatalogService.decide(context);

            assertThat(after.getRiskLevel()).isEqualTo(before.getRiskLevel());
            assertThat(after.getTotalScore()).isEqualTo(before.getTotalScore());
            assertThat(after.getDecidedBy()).isEqualTo(before.getDecidedBy());
            assertThat(after.getTriggeredRules()).hasSameSizeAs(before.getTriggeredRules());
        }
    }
}
