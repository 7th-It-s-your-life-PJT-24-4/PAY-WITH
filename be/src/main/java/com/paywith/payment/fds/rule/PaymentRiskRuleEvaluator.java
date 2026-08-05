package com.paywith.payment.fds.rule;

/**
 * 결제 점수 룰 평가기. 송금 {@code RiskRuleEvaluator}와 시그니처가 같지만 일부러 별도 타입이다 —
 * 송금 판정 서비스는 모든 송금 평가기 빈을 생성자에서 수집하므로, 같은 인터페이스를 구현하면
 * 결제 룰이 송금 평가기 맵에 흘러 들어간다. 타입을 분리해 빈 수집 경로 자체를 끊는다(양방향 동일).
 */
public interface PaymentRiskRuleEvaluator {

    String getRuleCode();

    boolean evaluate(PaymentRuleContext context);
}
