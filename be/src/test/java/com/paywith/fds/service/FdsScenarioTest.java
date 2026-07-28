package com.paywith.fds.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.paywith.fds.domain.RiskRule;
import com.paywith.fds.service.rule.FdsScoreResult;
import com.paywith.fds.service.rule.RiskRuleCache;
import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import com.paywith.fds.service.rule.TriggeredRule;
import com.paywith.fds.service.rule.impl.DivisionTransferRuleEvaluator;
import com.paywith.fds.service.rule.impl.HighAmountRuleEvaluator;
import com.paywith.fds.service.rule.impl.NewRecipientRuleEvaluator;
import com.paywith.fds.service.rule.impl.NightTimeRuleEvaluator;
import com.paywith.fds.service.rule.impl.RepeatedRuleEvaluator;
import com.paywith.fds.service.rule.impl.SafeAccountCheckRuleEvaluator;
import com.paywith.fds.service.rule.impl.SuspiciousMemoRuleEvaluator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 실제 룰 평가기 7종을 data.sql의 실제 배점·임계값(40)과 엮어, 현실적인 송금 시나리오별로
 * FdsEvaluationService.score()가 무엇을 반환하는지 확인한다. 실행하면 시나리오별 총점/보류/발동 룰이
 * 표로 출력되고, 각 시나리오의 기대 보류 판정을 단언한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("송금 FDS 시나리오")
class FdsScenarioTest {

    private static final int THRESHOLD = 40;

    // data.sql risk_rules 실제 배점 (rule_code -> score)
    private static final Map<String, Integer> RULE_SCORES = new LinkedHashMap<>() {{
        put("NEW_RECIPIENT", 24);
        put("HIGH_AMOUNT", 20);
        put("DIVISION_TRANSFER", 20);
        put("REPEATED", 16);
        put("NIGHT_TIME", 12);
        put("SUSPICIOUS_MEMO", 8);
        put("SAFE_ACCOUNT_CHECK", -40);
    }};

    private static final List<String> MEMO_KEYWORDS =
        List.of("검찰", "경찰", "수사", "안전계좌", "대출", "납치", "대포통장", "벌금");

    @Mock
    private RiskRuleCache riskRuleCache;

    // 실제 평가기 7종을 실제 설정값으로 생성 (application-local.properties 기본값과 동일)
    private final List<RiskRuleEvaluator> realEvaluators = List.of(
        new NewRecipientRuleEvaluator(),
        new HighAmountRuleEvaluator(500000L),
        new DivisionTransferRuleEvaluator(3),
        new RepeatedRuleEvaluator(3),
        new NightTimeRuleEvaluator(22, 6),
        new SuspiciousMemoRuleEvaluator(),
        new SafeAccountCheckRuleEvaluator()
    );

    // rule_id -> rule_code (출력 시 발동 룰 이름 복원용)
    private final Map<Long, String> ruleCodeById = new LinkedHashMap<>();

    private FdsEvaluationService realService() {
        List<RiskRule> activeRules = buildActiveRules();
        given(riskRuleCache.getActiveRules()).willReturn(activeRules);
        return new FdsEvaluationService(
            riskRuleCache, realEvaluators, null, null, null, null,
            THRESHOLD, 30, 30, MEMO_KEYWORDS.toArray(new String[0])
        );
    }

    private List<RiskRule> buildActiveRules() {
        long id = 1L;
        java.util.List<RiskRule> rules = new java.util.ArrayList<>();
        for (Map.Entry<String, Integer> entry : RULE_SCORES.entrySet()) {
            RiskRule rule = new RiskRule();
            rule.setRuleId(id);
            rule.setRuleCode(entry.getKey());
            rule.setScore(entry.getValue());
            rule.setActive(true);
            ruleCodeById.put(id, entry.getKey());
            rules.add(rule);
            id++;
        }
        return rules;
    }

    @Test
    @DisplayName("현실 시나리오별 점수/보류 판정을 출력하고 검증한다")
    void evaluatesRealisticTransferScenarios() {
        FdsEvaluationService service = realService();

        List<Scenario> scenarios = List.of(
            // 이름, 컨텍스트, 기대 보류여부
            new Scenario("단골에게 소액 주간 송금",
                ctx("30000", null, hour(14), 5, false, 0, 0), false),
            new Scenario("단골에게 고액 주간 송금",
                ctx("1000000", null, hour(14), 5, false, 0, 0), false),
            new Scenario("신규 수취인에게 고액 주간 송금",
                ctx("800000", null, hour(14), 0, false, 0, 0), true),
            new Scenario("신규+심야+위험메모(전형적 보이스피싱)",
                ctx("2000000", "검찰 수사 협조 요청", hour(23), 0, false, 0, 0), true),
            new Scenario("단시간 여러 계좌 분할 송금",
                ctx("300000", null, hour(11), 0, false, 3, 3), true),
            new Scenario("보호자 안전계좌로 신규 고액 송금",
                ctx("1000000", null, hour(14), 0, true, 0, 0), false),
            new Scenario("안전계좌+심야+위험메모(사회공학 우회 위험)",
                ctx("1000000", "안전계좌로 이체", hour(23), 0, true, 0, 0), false)
        );

        System.out.println();
        System.out.println("=== 송금 FDS 시나리오 결과 (임계값=" + THRESHOLD + ") ===");
        System.out.printf("%-38s | %5s | %-6s | %s%n", "시나리오", "총점", "보류", "발동 룰");
        System.out.println("-".repeat(110));

        for (Scenario s : scenarios) {
            FdsScoreResult result = service.score(s.context);
            String triggered = result.getTriggeredRules().stream()
                .map(TriggeredRule::getRuleId)
                .map(ruleCodeById::get)
                .collect(Collectors.joining(", "));
            System.out.printf("%-38s | %5d | %-6s | %s%n",
                s.name, result.getTotalScore(), result.isHeld() ? "보류" : "통과", triggered);

            assertThat(result.isHeld())
                .as("시나리오 '%s' 기대 보류=%s, 실제 총점=%d",
                    s.name, s.expectedHeld, result.getTotalScore())
                .isEqualTo(s.expectedHeld);
        }
        System.out.println("-".repeat(110));
    }

    private static RuleContext ctx(
        String amount, String memo, LocalDateTime at,
        int sendCount, boolean safe, int recentTransfers, int distinctRecipients
    ) {
        return new RuleContext(
            new BigDecimal(amount), memo, at, sendCount, safe,
            recentTransfers, distinctRecipients, MEMO_KEYWORDS
        );
    }

    private static LocalDateTime hour(int h) {
        return LocalDateTime.of(2026, 7, 24, h, 0);
    }

    private static final class Scenario {
        final String name;
        final RuleContext context;
        final boolean expectedHeld;

        Scenario(String name, RuleContext context, boolean expectedHeld) {
            this.name = name;
            this.context = context;
            this.expectedHeld = expectedHeld;
        }
    }
}
