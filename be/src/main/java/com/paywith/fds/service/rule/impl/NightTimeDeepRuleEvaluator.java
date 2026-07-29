package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 심야 송금: 자정 ~ deep-end-hour 직전(기본 00~05시).
 * NIGHT_TIME_LATE 와 시간대가 겹치지 않으므로 둘 중 하나만 발동한다.
 */
@Component
public class NightTimeDeepRuleEvaluator implements RiskRuleEvaluator {

    private final int deepEndHour;

    public NightTimeDeepRuleEvaluator(@Value("${fds.night.deep-end-hour}") int deepEndHour) {
        this.deepEndHour = deepEndHour;
    }

    @Override
    public String getRuleCode() {
        return "NIGHT_TIME_DEEP";
    }

    @Override
    public boolean evaluate(RuleContext context) {
        return context.getRequestedAt().getHour() < deepEndHour;
    }
}
