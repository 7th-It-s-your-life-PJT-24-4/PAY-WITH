package com.paywith.fds.support;

import com.paywith.fds.service.RiskGrader;
import com.paywith.fds.service.prefilter.FdsPreFilter;
import com.paywith.fds.service.prefilter.impl.RejectedRecipientPreFilter;
import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.impl.DivisionTransferRuleEvaluator;
import com.paywith.fds.service.rule.impl.HighAmountL1RuleEvaluator;
import com.paywith.fds.service.rule.impl.HighAmountL2RuleEvaluator;
import com.paywith.fds.service.rule.impl.HighAmountL3RuleEvaluator;
import com.paywith.fds.service.rule.impl.NewRecipientRuleEvaluator;
import com.paywith.fds.service.rule.impl.NightTimeDeepRuleEvaluator;
import com.paywith.fds.service.rule.impl.NightTimeLateRuleEvaluator;
import com.paywith.fds.service.rule.impl.PendingApprovalRuleEvaluator;
import com.paywith.fds.service.rule.impl.RepeatedRuleEvaluator;
import com.paywith.fds.service.rule.impl.SafeAccountCheckRuleEvaluator;
import com.paywith.fds.service.rule.impl.SuspiciousMemoRuleEvaluator;
import java.util.List;

/**
 * application-local.properties 기본값으로 실제 평가기·필터·등급 판정부를 조립한다.
 * 설정값이 바뀌면 이 상수들과 properties 를 함께 고쳐야 한다.
 */
public final class FdsTestWiring {

    public static final int CAUTION_THRESHOLD = 25;
    public static final int DANGER_THRESHOLD = 50;

    private static final long L1 = 500_000L;
    private static final long L2 = 1_500_000L;
    private static final long L3 = 3_000_000L;

    private FdsTestWiring() {
    }

    public static RiskGrader grader() {
        return new RiskGrader(CAUTION_THRESHOLD, DANGER_THRESHOLD);
    }

    public static List<RiskRuleEvaluator> evaluators() {
        return List.of(
            new NewRecipientRuleEvaluator(),
            new HighAmountL1RuleEvaluator(L1, L2),
            new HighAmountL2RuleEvaluator(L2, L3),
            new HighAmountL3RuleEvaluator(L3),
            new NightTimeLateRuleEvaluator(22),
            new NightTimeDeepRuleEvaluator(6),
            new SuspiciousMemoRuleEvaluator(),
            new PendingApprovalRuleEvaluator(),
            new RepeatedRuleEvaluator(3),
            new DivisionTransferRuleEvaluator(3),
            new SafeAccountCheckRuleEvaluator()
        );
    }

    /** 운영과 동일한 @Order 순서를 재현한다. */
    public static List<FdsPreFilter> preFilters() {
        return List.of(new RejectedRecipientPreFilter());
    }
}
