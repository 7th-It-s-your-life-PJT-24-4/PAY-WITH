package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import org.springframework.stereotype.Component;

@Component
public class SafeAccountCheckRuleEvaluator implements RiskRuleEvaluator {

    @Override
    public String getRuleCode() {
        return "SAFE_ACCOUNT_CHECK";
    }

    @Override
    public boolean evaluate(RuleContext context) {
        return context.isRecipientRegisteredSafe();
    }
}
