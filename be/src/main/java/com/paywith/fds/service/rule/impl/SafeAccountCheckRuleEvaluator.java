package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import org.springframework.stereotype.Component;

/**
 * 안전계좌로 등록된 수취인에 대한 감점. 등록 주체(시니어 본인 / 보호자)는 구분하지 않는다.
 *
 * <p>감점에 그치고 무조건 통과시키지는 않는다. 등록 자체를 사회공학으로 유도할 수 있으므로
 * 다른 위험 신호가 겹치면 감점을 뚫고 등급이 올라가야 한다.
 *
 * <p>보호자 등록에 더 큰 감점을 주려면 recipients.safe_registered_by 로 룰을 둘로 나눌 수 있다.
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
