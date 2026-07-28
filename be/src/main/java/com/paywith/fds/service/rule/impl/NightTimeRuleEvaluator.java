package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NightTimeRuleEvaluator implements RiskRuleEvaluator {

    private final int startHour;
    private final int endHour;

    public NightTimeRuleEvaluator(
        @Value("${fds.night-start-hour}") int startHour,
        @Value("${fds.night-end-hour}") int endHour
    ) {
        this.startHour = startHour;
        this.endHour = endHour;
    }

    @Override
    public String getRuleCode() {
        return "NIGHT_TIME";
    }

    @Override
    public boolean evaluate(RuleContext context) {
        int hour = context.getRequestedAt().getHour();
        // 자정을 넘어가는 구간(예: 22시~06시)은 시작 > 종료로 표현되므로 OR로, 아니면 AND로 판정한다.
        if (startHour > endHour) {
            return hour >= startHour || hour < endHour;
        }
        return hour >= startHour && hour < endHour;
    }
}
