package com.paywith.fds.service.rule.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 고액 2구간: L2 이상 ~ L3 미만. */
@Component
public class HighAmountL2RuleEvaluator extends AmountRangeRuleEvaluator {

    public HighAmountL2RuleEvaluator(
        @Value("${fds.high-amount.l2-threshold}") long l2Threshold,
        @Value("${fds.high-amount.l3-threshold}") long l3Threshold
    ) {
        super(l2Threshold, l3Threshold);
    }

    @Override
    public String getRuleCode() {
        return "HIGH_AMOUNT_L2";
    }
}
