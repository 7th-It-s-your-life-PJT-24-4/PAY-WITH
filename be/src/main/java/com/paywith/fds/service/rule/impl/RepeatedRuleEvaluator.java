package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RepeatedRuleEvaluator implements RiskRuleEvaluator {

    private final int countThreshold;

    public RepeatedRuleEvaluator(@Value("${fds.repeated.count-threshold}") int countThreshold) {
        this.countThreshold = countThreshold;
    }

    @Override
    public String getRuleCode() {
        return "REPEATED";
    }

    @Override
    public boolean evaluate(RuleContext context) {
        return context.getRecentTransferCount() >= countThreshold;
    }
}
