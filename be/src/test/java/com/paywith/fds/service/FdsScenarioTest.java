package com.paywith.fds.service;

import static com.paywith.fds.support.RuleContexts.at;
import static com.paywith.fds.support.RuleContexts.normal;
import static com.paywith.fds.support.RuleContexts.won;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import com.paywith.fds.domain.DecidedBy;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.domain.RiskRule;
import com.paywith.fds.dto.FdsDecision;
import com.paywith.fds.dto.TriggeredRule;
import com.paywith.fds.service.rule.RiskRuleCache;
import com.paywith.fds.service.rule.RuleContext;
import com.paywith.fds.support.FdsTestWiring;
import com.paywith.fds.support.RiskRules;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 실제 평가기·필터·배점을 엮어 현실적인 송금 시나리오별 등급 판정을 확인한다.
 * 실행하면 시나리오별 총점·등급·판정경로·발동 룰이 표로 출력된다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("송금 FDS 시나리오")
class FdsScenarioTest {

    @Mock
    private RiskRuleCache riskRuleCache;

    private FdsEvaluationServiceImpl service;
    private Map<Long, String> ruleCodeById;

    @BeforeEach
    void setUp() {
        List<RiskRule> activeRules = RiskRules.activeRules();
        ruleCodeById = RiskRules.codeById(activeRules);

        lenient().when(riskRuleCache.getActiveRules()).thenReturn(activeRules);
        for (RiskRule rule : activeRules) {
            lenient().when(riskRuleCache.findByCode(rule.getRuleCode()))
                .thenReturn(Optional.of(rule));
        }

        service = new FdsEvaluationServiceImpl(
            null,
            riskRuleCache,
            FdsTestWiring.grader(),
            FdsTestWiring.preFilters(),
            FdsTestWiring.evaluators(),
            null, null, null, null
        );
    }

    @Test
    @DisplayName("현실 시나리오별 등급 판정을 출력하고 검증한다")
    void evaluatesRealisticTransferScenarios() {
        List<Scenario> scenarios = List.of(
            new Scenario("단골에게 소액 주간 송금",
                normal().build(), RiskLevel.SAFE),

            new Scenario("단골에게 100만원 주간 송금",
                normal().amount(won("1000000")).build(), RiskLevel.SAFE),

            // 경조사·병원비 등 시니어의 가장 흔한 정상 패턴이므로 차단하지 않는다
            new Scenario("신규 수취인에게 80만원 주간 송금",
                normal().recipientSendCount(0).amount(won("800000")).build(), RiskLevel.CAUTION),

            new Scenario("신규 수취인에게 300만원 주간 송금",
                normal().recipientSendCount(0).amount(won("3000000")).build(), RiskLevel.DANGER),

            new Scenario("신규+심야+검찰메모+200만원(전형적 보이스피싱)",
                normal().recipientSendCount(0).amount(won("2000000"))
                    .memo("검찰 수사 협조 요청").requestedAt(at(23)).build(), RiskLevel.DANGER),

            new Scenario("분할 3계좌+반복 3회+신규 수취인",
                normal().recipientSendCount(0).amount(won("300000"))
                    .recentDistinctRecipientCount(3).recentTransferCount(3).build(), RiskLevel.DANGER),

            // 본인 등록 안전계좌는 감점에 그치므로 사회공학 우회가 통하지 않는다
            new Scenario("본인 안전계좌+심야+검찰메모+100만원",
                normal().recipientRegisteredSafe(true).amount(won("1000000"))
                    .memo("검찰 수사 협조 요청").requestedAt(at(2)).build(), RiskLevel.CAUTION),

            // 승인 대기 건이 있어도 다른 송금은 막지 않되, 경계 수위만 한 단계 올린다
            new Scenario("승인대기 중 단골에게 소액 송금",
                normal().pendingApprovalExists(true).build(), RiskLevel.SAFE),

            new Scenario("승인대기 중 단골에게 100만원 송금",
                normal().pendingApprovalExists(true).amount(won("1000000")).build(),
                RiskLevel.CAUTION),

            // 안전계좌 감점이 신규·고액 가점을 상쇄한다(무조건 통과가 아니라 저울질)
            new Scenario("안전계좌로 신규 고액 송금",
                normal().recipientRegisteredSafe(true).recipientSendCount(0)
                    .amount(won("1000000")).build(), RiskLevel.SAFE),

            new Scenario("보호자가 거절했던 계좌로 재송금",
                normal().recipientRejectedBefore(true).build(), RiskLevel.DANGER)
        );

        System.out.println();
        System.out.printf("=== 송금 FDS 시나리오 (주의=%d / 위험=%d) ===%n",
            FdsTestWiring.CAUTION_THRESHOLD, FdsTestWiring.DANGER_THRESHOLD);
        System.out.printf("%-42s | %4s | %-7s | %-9s | %s%n",
            "시나리오", "총점", "등급", "판정경로", "발동 룰");
        System.out.println("-".repeat(130));

        for (Scenario scenario : scenarios) {
            FdsDecision decision = service.decide(scenario.context);
            String triggered = decision.getTriggeredRules().stream()
                .map(TriggeredRule::getRuleId)
                .map(ruleCodeById::get)
                .collect(Collectors.joining(", "));

            System.out.printf("%-42s | %4d | %-7s | %-9s | %s%n",
                scenario.name, decision.getTotalScore(), decision.getRiskLevel(),
                decision.getDecidedBy(), triggered);

            assertThat(decision.getRiskLevel())
                .as("시나리오 '%s' — 총점 %d, 발동 룰 [%s]",
                    scenario.name, decision.getTotalScore(), triggered)
                .isEqualTo(scenario.expectedLevel);
        }
        System.out.println("-".repeat(130));
    }

    @Test
    void shortCircuitedDecision_carriesZeroScoreAndPrefilterPath() {
        FdsDecision decision = service.decide(normal().recipientRejectedBefore(true).build());

        // 단축평가는 점수와 무관하게 확정되므로 총점을 등급 해석에 쓰면 안 된다
        assertThat(decision.getTotalScore()).isZero();
        assertThat(decision.getDecidedBy()).isEqualTo(DecidedBy.BLACKLIST);
        assertThat(decision.getTriggeredRules()).hasSize(1);
        assertThat(ruleCodeById.get(decision.getTriggeredRules().get(0).getRuleId()))
            .isEqualTo("BL_REJECTED_RECIPIENT");
    }

    // 안전계좌 감점이 있어도 블랙리스트는 무조건 차단한다
    @Test
    void blacklistOverridesSafeAccountDiscount() {
        FdsDecision decision = service.decide(normal()
            .recipientRegisteredSafe(true)
            .recipientRejectedBefore(true)
            .build());

        assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.DANGER);
        assertThat(decision.getDecidedBy()).isEqualTo(DecidedBy.BLACKLIST);
    }

    private static final class Scenario {
        final String name;
        final RuleContext context;
        final RiskLevel expectedLevel;

        Scenario(String name, RuleContext context, RiskLevel expectedLevel) {
            this.name = name;
            this.context = context;
            this.expectedLevel = expectedLevel;
        }
    }
}
