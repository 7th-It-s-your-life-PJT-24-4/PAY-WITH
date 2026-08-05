package com.paywith.payment.fds.support;

import com.paywith.fds.service.RiskGrader;
import com.paywith.payment.fds.rule.PaymentRiskRuleEvaluator;
import com.paywith.payment.fds.rule.impl.PaymentGiftCardAmountRuleEvaluator;
import com.paywith.payment.fds.rule.impl.PaymentHighAmountL1RuleEvaluator;
import com.paywith.payment.fds.rule.impl.PaymentHighAmountL2RuleEvaluator;
import com.paywith.payment.fds.rule.impl.PaymentHighAmountL3RuleEvaluator;
import com.paywith.payment.fds.rule.impl.PaymentImpossibleTravelEvaluator;
import com.paywith.payment.fds.rule.impl.PaymentNightDeepRuleEvaluator;
import com.paywith.payment.fds.rule.impl.PaymentNightLateRuleEvaluator;
import com.paywith.payment.fds.rule.impl.PaymentPendingApprovalRuleEvaluator;
import com.paywith.payment.fds.rule.impl.PaymentRiskyCategoryRuleEvaluator;
import com.paywith.payment.fds.rule.impl.PaymentSplitPaymentRuleEvaluator;
import java.util.List;

/** application-local.properties 기본값으로 조립한다. 설정이 바뀌면 함께 고쳐야 한다. */
public final class PaymentFdsTestWiring {

    /** 등급 임계값은 송금과 공유(D6). */
    public static final int CAUTION_THRESHOLD = 25;
    public static final int DANGER_THRESHOLD = 50;

    public static final long L1 = 100_000L;
    public static final long L2 = 300_000L;
    public static final long L3 = 500_000L;
    public static final int LATE_START_HOUR = 22;
    public static final int DEEP_END_HOUR = 6;
    public static final int SPLIT_COUNT_THRESHOLD = 2;
    public static final double IMPOSSIBLE_SPEED_KMH = 300.0;

    private PaymentFdsTestWiring() {
    }

    public static RiskGrader grader() {
        return new RiskGrader(CAUTION_THRESHOLD, DANGER_THRESHOLD);
    }

    public static PaymentImpossibleTravelEvaluator travelEvaluator() {
        return new PaymentImpossibleTravelEvaluator(IMPOSSIBLE_SPEED_KMH);
    }

    public static List<PaymentRiskRuleEvaluator> evaluators() {
        return List.of(
            new PaymentHighAmountL1RuleEvaluator(L1, L2),
            new PaymentHighAmountL2RuleEvaluator(L2, L3),
            new PaymentHighAmountL3RuleEvaluator(L3),
            new PaymentNightLateRuleEvaluator(LATE_START_HOUR),
            new PaymentNightDeepRuleEvaluator(DEEP_END_HOUR),
            new PaymentPendingApprovalRuleEvaluator(),
            new PaymentRiskyCategoryRuleEvaluator(),
            new PaymentGiftCardAmountRuleEvaluator(SPLIT_COUNT_THRESHOLD),
            new PaymentSplitPaymentRuleEvaluator(SPLIT_COUNT_THRESHOLD)
        );
    }
}
