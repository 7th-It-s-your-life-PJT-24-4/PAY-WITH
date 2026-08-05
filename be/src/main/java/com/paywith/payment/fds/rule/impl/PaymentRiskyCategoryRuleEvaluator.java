package com.paywith.payment.fds.rule.impl;

import com.paywith.payment.fds.rule.PaymentRiskRuleEvaluator;
import com.paywith.payment.fds.rule.PaymentRuleContext;
import org.springframework.stereotype.Component;

/**
 * 위험 업종 결제 — 귀금속·전자제품·상품권 전문·명품관 등 현금 교환이 쉬운 물품 판매 업종.
 * 단독 발동(25점)이 정확히 CAUTION 문턱이라 위험 업종 결제는 매 건 보호자에게 알려진다 —
 * 위험 업종엔 반복 룰이 없으므로 반복 구매 패턴은 알림 누적으로 보호자가 보게 하는 설계.
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
        String categoryCode = context.getMerchantCategoryCode();
        return categoryCode != null && context.getRiskyCategories().contains(categoryCode);
    }
}
