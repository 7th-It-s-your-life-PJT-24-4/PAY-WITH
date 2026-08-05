package com.paywith.payment.fds.rule.impl;

import com.paywith.payment.fds.rule.PaymentRiskRuleEvaluator;
import com.paywith.payment.fds.rule.PaymentRuleContext;

/** 구간이 [하한, 상한)으로 겹치지 않아 L1/L2/L3 중 최대 하나만 발동한다(송금 금액 룰과 동일 규약). */
abstract class PaymentAmountRangeRuleEvaluator implements PaymentRiskRuleEvaluator {

    private final long minInclusive;
    private final Long maxExclusive;

    /** @param maxExclusive 상한 없음(최고 구간)이면 null */
    protected PaymentAmountRangeRuleEvaluator(long minInclusive, Long maxExclusive) {
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
    }

    @Override
    public boolean evaluate(PaymentRuleContext context) {
        long amount = context.getAmount();
        if (amount < minInclusive) {
            return false;
        }
        return maxExclusive == null || amount < maxExclusive;
    }
}
