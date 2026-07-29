package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DivisionTransferRuleEvaluator implements RiskRuleEvaluator {

    private final int accountThreshold;

    public DivisionTransferRuleEvaluator(@Value("${fds.division.account-threshold}") int accountThreshold) {
        this.accountThreshold = accountThreshold;
    }

    @Override
    public String getRuleCode() {
        return "DIVISION_TRANSFER";
    }

    @Override
    public boolean evaluate(RuleContext context) {
        return context.getRecentDistinctRecipientCount() >= accountThreshold;
    }
}
