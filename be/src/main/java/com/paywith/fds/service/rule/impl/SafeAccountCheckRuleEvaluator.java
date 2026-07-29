package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import org.springframework.stereotype.Component;

/**
 * 안전계좌 감점. 등록 자체를 사회공학으로 유도할 수 있어 무조건 통과시키지는 않고,
 * 다른 위험 신호가 겹치면 감점을 뚫고 등급이 올라가게 둔다.
 */
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
