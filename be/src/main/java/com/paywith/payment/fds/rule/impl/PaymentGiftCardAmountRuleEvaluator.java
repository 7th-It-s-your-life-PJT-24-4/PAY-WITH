package com.paywith.payment.fds.rule.impl;

import com.paywith.payment.fds.rule.PaymentRiskRuleEvaluator;
import com.paywith.payment.fds.rule.PaymentRuleContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 상품권 의심 결제 — 상품권 취급 업종(편의점·마트 등, 위험 업종 아님)에서 단위 배수 금액.
 * SPLIT_PAYMENT 는 이 룰의 상위 티어(전제 조건 포함)라 같은 계열에서 한 행만 발동한다 —
 * 반복 조건까지 충족하면 SPLIT 만, 아니면 이 룰만.
 */
@Component
public class PaymentGiftCardAmountRuleEvaluator implements PaymentRiskRuleEvaluator {

    private final int splitCountThreshold;

    public PaymentGiftCardAmountRuleEvaluator(
        @Value("${fds.payment.split.count-threshold}") int splitCountThreshold
    ) {
        this.splitCountThreshold = splitCountThreshold;
    }

    @Override
    public String getRuleCode() {
        return "PAY_GIFT_CARD_AMOUNT";
    }

    @Override
    public boolean evaluate(PaymentRuleContext context) {
        return context.isGiftCardSuspect()
            && context.getGiftCardSuspectRecentCount() < splitCountThreshold;
    }
}
