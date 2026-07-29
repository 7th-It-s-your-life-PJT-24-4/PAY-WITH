package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import java.math.BigDecimal;

/**
 * 금액 구간 룰의 공통 판정부. 구간은 [하한, 상한)으로 서로 겹치지 않으므로
 * HIGH_AMOUNT_L1/L2/L3 중 최대 하나만 발동한다.
 */
abstract class AmountRangeRuleEvaluator implements RiskRuleEvaluator {

    private final BigDecimal minInclusive;
    private final BigDecimal maxExclusive;

    /** @param maxExclusive 상한 없음(최고 구간)이면 null */
    protected AmountRangeRuleEvaluator(long minInclusive, Long maxExclusive) {
        this.minInclusive = BigDecimal.valueOf(minInclusive);
        this.maxExclusive = maxExclusive == null ? null : BigDecimal.valueOf(maxExclusive);
    }

    @Override
    public boolean evaluate(RuleContext context) {
        BigDecimal amount = context.getAmount();
        if (amount.compareTo(minInclusive) < 0) {
            return false;
        }
        return maxExclusive == null || amount.compareTo(maxExclusive) < 0;
    }
}
