package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import java.math.BigDecimal;

/** 구간이 [하한, 상한)으로 겹치지 않아 L1/L2/L3 중 최대 하나만 발동한다. */
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
