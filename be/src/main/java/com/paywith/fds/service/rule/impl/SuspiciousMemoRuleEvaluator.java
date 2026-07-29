package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 메모에 위험 키워드가 포함된 경우. 키워드 종류나 개수에 관계없이 동일 배점이다.
 */
@Component
public class SuspiciousMemoRuleEvaluator implements RiskRuleEvaluator {

    @Override
    public String getRuleCode() {
        return "SUSPICIOUS_MEMO";
    }

    @Override
    public boolean evaluate(RuleContext context) {
        String memo = context.getMemo();
        if (!StringUtils.hasText(memo) || context.getMemoKeywords() == null) {
            return false;
        }
        return context.getMemoKeywords().stream().anyMatch(memo::contains);
    }
}
