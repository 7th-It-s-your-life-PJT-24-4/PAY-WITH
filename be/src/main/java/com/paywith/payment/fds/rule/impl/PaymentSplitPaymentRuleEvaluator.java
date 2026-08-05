package com.paywith.payment.fds.rule.impl;

import com.paywith.payment.fds.rule.PaymentRiskRuleEvaluator;
import com.paywith.payment.fds.rule.PaymentRuleContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 상품권 의심 결제의 단기간 반복 — 고액 FDS 회피 목적의 분할 결제로 판단(송금 DIVISION_TRANSFER 의
 * 결제판). 배점 40은 분할(40+L1=50 차단)이 일괄 구매(L3+GIFT=45 알림)보다 불리하도록 정한 값 —
 * 회피 인센티브를 없애는 산수라 임의로 낮추면 안 된다.
 *
 * <p>과거 건수는 윈도 내 COMPLETED 한정으로 집계된다(분할 회피는 이전 건들이 통과에 성공했다는
 * 뜻). 현재 결제는 평가 시점에 거래 행이 아직 없어 집계에 포함되지 않으므로, 임계 2는 현재 건을
 * 포함해 3건째부터 발동한다는 의미다.
 */
@Component
public class PaymentSplitPaymentRuleEvaluator implements PaymentRiskRuleEvaluator {

    private final int splitCountThreshold;

    public PaymentSplitPaymentRuleEvaluator(
        @Value("${fds.payment.split.count-threshold}") int splitCountThreshold
    ) {
        this.splitCountThreshold = splitCountThreshold;
    }

    @Override
    public String getRuleCode() {
        return "PAY_SPLIT_PAYMENT";
    }

    @Override
    public boolean evaluate(PaymentRuleContext context) {
        return context.isGiftCardSuspect()
            && context.getGiftCardSuspectRecentCount() >= splitCountThreshold;
    }
}
