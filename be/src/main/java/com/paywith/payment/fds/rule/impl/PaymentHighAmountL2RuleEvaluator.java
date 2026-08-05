package com.paywith.payment.fds.rule.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 고액 결제 2구간: L2 이상 ~ L3 미만. */
@Component
public class PaymentHighAmountL2RuleEvaluator extends PaymentAmountRangeRuleEvaluator {

    public PaymentHighAmountL2RuleEvaluator(
        @Value("${fds.payment.high-amount.l2}") long l2Threshold,
        @Value("${fds.payment.high-amount.l3}") long l3Threshold
    ) {
        super(l2Threshold, l3Threshold);
    }

    @Override
    public String getRuleCode() {
        return "PAY_HIGH_AMOUNT_L2";
    }
}
