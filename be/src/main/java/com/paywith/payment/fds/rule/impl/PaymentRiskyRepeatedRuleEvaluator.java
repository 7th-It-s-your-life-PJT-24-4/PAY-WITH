package com.paywith.payment.fds.rule.impl;

import com.paywith.payment.fds.rule.PaymentRiskRuleEvaluator;
import com.paywith.payment.fds.rule.PaymentRuleContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 위험 업종 결제의 단기간 반복 — 고액 차단(RISKY+L3=120→상한 100)을 금액 분할로 피하는
 * 패턴을 잡는다(SPLIT_PAYMENT 의 위험 업종판. isGiftCardSuspect 가 위험 업종을 배제하므로
 * SPLIT 은 이 패턴을 보지 못한다). 배점 40은 40만 반복 3건째(RISKY 50+L2 36+40=126→상한
 * 100 차단)가 차단으로 넘어가되, 소액 반복(RISKY 50+40=90)은 주의에 머물도록 정한 값 —
 * 정당한 재방문을 과차단하지 않는 산수라 임의로 바꾸면 안 된다.
 *
 * <p>과거 건수는 윈도 내 COMPLETED 한정으로 집계된다(분할 회피는 이전 건들이 통과에 성공했다는
 * 뜻). 현재 결제는 평가 시점에 거래 행이 아직 없어 집계에 포함되지 않으므로, 임계 2는 현재 건을
 * 포함해 3건째부터 발동한다는 의미다.
 */
@Component
public class PaymentRiskyRepeatedRuleEvaluator implements PaymentRiskRuleEvaluator {

    private final int riskyRepeatedCountThreshold;

    public PaymentRiskyRepeatedRuleEvaluator(
        @Value("${fds.payment.risky-repeated.count-threshold}") int riskyRepeatedCountThreshold
    ) {
        this.riskyRepeatedCountThreshold = riskyRepeatedCountThreshold;
    }

    @Override
    public String getRuleCode() {
        return "PAY_RISKY_REPEATED";
    }

    @Override
    public boolean evaluate(PaymentRuleContext context) {
        return context.isRiskyCategory()
            && context.getRiskyCategoryRecentCount() >= riskyRepeatedCountThreshold;
    }
}
