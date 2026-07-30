package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import org.springframework.stereotype.Component;

/**
 * 승인 대기 중 추가 송금. 하나가 막히자 다른 계좌로 시도하는 패턴을 잡되, 차단은 하지 않고
 * 가점으로 경계 수위만 올린다.
 */
@Component
public class PendingApprovalRuleEvaluator implements RiskRuleEvaluator {

    @Override
    public String getRuleCode() {
        return "PENDING_APPROVAL_EXISTS";
    }

    @Override
    public boolean evaluate(RuleContext context) {
        return context.isPendingApprovalExists();
    }
}
