package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SuspiciousMemoRuleEvaluator implements RiskRuleEvaluator {

    @Override
    public String getRuleCode() {
        return "SUSPICIOUS_MEMO";
    }

    @Override
    public boolean evaluate(RuleContext context) {
        String memo = context.getMemo();
        if (!StringUtils.hasText(memo)) {
            return false;
        }
        return context.getMemoKeywords().stream().anyMatch(memo::contains);
    }
}
