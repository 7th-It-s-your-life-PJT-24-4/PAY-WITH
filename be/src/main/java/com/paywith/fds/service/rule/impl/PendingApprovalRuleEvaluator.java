package com.paywith.fds.service.rule.impl;

import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import org.springframework.stereotype.Component;

/**
 * 보호자 승인 대기 중인 송금이 남아 있는 상태에서의 추가 송금.
 *
 * <p>차단하지는 않는다. 승인 대기 건과 무관한 정상 송금을 막으면 사용성이 크게 나빠진다.
 * 대신 "한 건이 이미 걸린 상황"이라는 맥락을 가점으로 반영해 평소보다 경계 수위를 올린다.
 * 하나가 막히자 다른 계좌로 시도하는 패턴을 잡기 위한 것이다.
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
