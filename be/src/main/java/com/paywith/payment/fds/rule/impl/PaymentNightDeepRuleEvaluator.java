package com.paywith.payment.fds.rule.impl;

import com.paywith.payment.fds.rule.PaymentRiskRuleEvaluator;
import com.paywith.payment.fds.rule.PaymentRuleContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 자정 ~ deep-end-hour 직전(기본 00~05시). LATE 와 겹치지 않는다. 시간 구간은 송금 fds.night.* 공용. */
@Component
public class PaymentNightDeepRuleEvaluator implements PaymentRiskRuleEvaluator {

    private final int deepEndHour;

    public PaymentNightDeepRuleEvaluator(@Value("${fds.night.deep-end-hour}") int deepEndHour) {
        this.deepEndHour = deepEndHour;
    }

    @Override
    public String getRuleCode() {
        return "PAY_NIGHT_DEEP";
    }

    @Override
    public boolean evaluate(PaymentRuleContext context) {
        return context.getRequestedAt().getHour() < deepEndHour;
    }
}
