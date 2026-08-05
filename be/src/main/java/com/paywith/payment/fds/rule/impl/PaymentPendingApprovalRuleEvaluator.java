package com.paywith.payment.fds.rule.impl;

import com.paywith.payment.fds.rule.PaymentRiskRuleEvaluator;
import com.paywith.payment.fds.rule.PaymentRuleContext;
import org.springframework.stereotype.Component;

/**
 * 승인 대기 송금이 있는 상태의 결제. 송금이 막힌 상태에서 결제로 자금을 빼는 패턴을 잡되,
 * 차단은 하지 않고 가점으로 경계 수위만 올린다(송금 PENDING_APPROVAL_EXISTS의 결제판).
 */
@Component
public class PaymentPendingApprovalRuleEvaluator implements PaymentRiskRuleEvaluator {

    @Override
    public String getRuleCode() {
        return "PAY_PENDING_APPROVAL";
    }

    @Override
    public boolean evaluate(PaymentRuleContext context) {
        return context.isPendingApprovalExists();
    }
}
