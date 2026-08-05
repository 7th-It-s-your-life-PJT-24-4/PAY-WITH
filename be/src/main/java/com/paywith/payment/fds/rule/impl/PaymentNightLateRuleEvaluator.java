package com.paywith.payment.fds.rule.impl;

import com.paywith.payment.fds.rule.PaymentRiskRuleEvaluator;
import com.paywith.payment.fds.rule.PaymentRuleContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** late-start-hour ~ 자정 직전(기본 22~23시). DEEP 과 겹치지 않는다. 시간 구간은 송금 fds.night.* 공용. */
@Component
public class PaymentNightLateRuleEvaluator implements PaymentRiskRuleEvaluator {

    private final int lateStartHour;

    public PaymentNightLateRuleEvaluator(@Value("${fds.night.late-start-hour}") int lateStartHour) {
        this.lateStartHour = lateStartHour;
    }

    @Override
    public String getRuleCode() {
        return "PAY_NIGHT_LATE";
    }

    @Override
    public boolean evaluate(PaymentRuleContext context) {
        return context.getRequestedAt().getHour() >= lateStartHour;
    }
}
