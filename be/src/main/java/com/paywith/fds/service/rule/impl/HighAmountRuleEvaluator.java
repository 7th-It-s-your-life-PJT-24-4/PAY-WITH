package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HighAmountRuleEvaluator implements RiskRuleEvaluator {

    private final BigDecimal threshold;

    public HighAmountRuleEvaluator(@Value("${fds.high-amount-threshold}") long threshold) {
        this.threshold = BigDecimal.valueOf(threshold);
    }

    @Override
    public String getRuleCode() {
        return "HIGH_AMOUNT";
    }

    @Override
    public boolean evaluate(RuleContext context) {
        return context.getAmount().compareTo(threshold) >= 0;
    }
}
