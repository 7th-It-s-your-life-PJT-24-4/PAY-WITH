package com.paywith.fds.service.rule.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 고액 1구간: L1 이상 ~ L2 미만. */
@Component
public class HighAmountL1RuleEvaluator extends AmountRangeRuleEvaluator {

    public HighAmountL1RuleEvaluator(
        @Value("${fds.high-amount.l1-threshold}") long l1Threshold,
        @Value("${fds.high-amount.l2-threshold}") long l2Threshold
    ) {
        super(l1Threshold, l2Threshold);
    }

    @Override
    public String getRuleCode() {
        return "HIGH_AMOUNT_L1";
    }
}
