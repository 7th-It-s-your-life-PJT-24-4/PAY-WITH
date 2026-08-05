package com.paywith.payment.fds.rule.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 고액 결제 3구간: L3 이상(상한 없음). 개발 시드 잔액 50만 원으로 정확히 L3 경계를 시연할 수 있다. */
@Component
public class PaymentHighAmountL3RuleEvaluator extends PaymentAmountRangeRuleEvaluator {

    public PaymentHighAmountL3RuleEvaluator(@Value("${fds.payment.high-amount.l3}") long l3Threshold) {
        super(l3Threshold, null);
    }

    @Override
    public String getRuleCode() {
        return "PAY_HIGH_AMOUNT_L3";
    }
}
