package com.paywith.payment.fds.rule.impl;

import com.paywith.payment.fds.rule.PaymentRiskRuleEvaluator;
import com.paywith.payment.fds.rule.PaymentRuleContext;
import org.springframework.stereotype.Component;

/**
 * 위험 업종 결제 — 귀금속·전자제품·상품권 전문·명품관 등 현금 교환이 쉬운 물품 판매 업종.
 * 단독 발동(50점)이 정확히 CAUTION 문턱이라 위험 업종 결제는 매 건 보호자에게 알려지고,
 * 단기간 반복 구매는 RISKY_REPEATED 가 가점으로 잡는다(위험 업종 판정식은 context 공유).
 * category_code NULL 이면 스킵(데이터 미비 케이스).
 */
@Component
public class PaymentRiskyCategoryRuleEvaluator implements PaymentRiskRuleEvaluator {

    @Override
    public String getRuleCode() {
        return "PAY_RISKY_CATEGORY";
    }

    @Override
    public boolean evaluate(PaymentRuleContext context) {
        return context.isRiskyCategory();
    }
}
