package com.paywith.payment.fds.rule.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 고액 결제 1구간: L1 이상 ~ L2 미만. */
@Component
public class PaymentHighAmountL1RuleEvaluator extends PaymentAmountRangeRuleEvaluator {

    public PaymentHighAmountL1RuleEvaluator(
        @Value("${fds.payment.high-amount.l1}") long l1Threshold,
        @Value("${fds.payment.high-amount.l2}") long l2Threshold
    ) {
        super(l1Threshold, l2Threshold);
    }

    @Override
    public String getRuleCode() {
        return "PAY_HIGH_AMOUNT_L1";
    }
}
