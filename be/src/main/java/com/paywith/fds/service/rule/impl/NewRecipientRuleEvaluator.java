package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import org.springframework.stereotype.Component;

@Component
public class NewRecipientRuleEvaluator implements RiskRuleEvaluator {

    @Override
    public String getRuleCode() {
        return "NEW_RECIPIENT";
    }

    @Override
    public boolean evaluate(RuleContext context) {
        return context.getRecipientSendCount() == 0;
    }
}
