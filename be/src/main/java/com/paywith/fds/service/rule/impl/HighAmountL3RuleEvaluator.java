package com.paywith.fds.service.rule.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 고액 3구간: L3 이상(상한 없음). */
@Component
public class HighAmountL3RuleEvaluator extends AmountRangeRuleEvaluator {

    public HighAmountL3RuleEvaluator(@Value("${fds.high-amount.l3-threshold}") long l3Threshold) {
        super(l3Threshold, null);
    }

    @Override
    public String getRuleCode() {
        return "HIGH_AMOUNT_L3";
    }
}
