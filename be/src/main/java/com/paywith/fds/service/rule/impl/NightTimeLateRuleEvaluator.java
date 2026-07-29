package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** late-start-hour ~ 자정 직전(기본 22~23시). DEEP 과 겹치지 않는다. */
@Component
public class NightTimeLateRuleEvaluator implements RiskRuleEvaluator {

    private final int lateStartHour;

    public NightTimeLateRuleEvaluator(@Value("${fds.night.late-start-hour}") int lateStartHour) {
        this.lateStartHour = lateStartHour;
    }

    @Override
    public String getRuleCode() {
        return "NIGHT_TIME_LATE";
    }

    @Override
    public boolean evaluate(RuleContext context) {
        return context.getRequestedAt().getHour() >= lateStartHour;
    }
}
