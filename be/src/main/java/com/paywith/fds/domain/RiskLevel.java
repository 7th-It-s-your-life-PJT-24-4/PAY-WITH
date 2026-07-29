package com.paywith.fds.domain;

/**
 * 송금 위험 등급. risk_evaluations.risk_level 과 이름이 일치한다.
 *
 * <p>처리 단계(transactions.status)와는 별개 축이다. SAFE 와 CAUTION 은 똑같이 송금이 진행되고,
 * 주의 판정을 받았다는 사실은 이 값으로만 남는다.
 */
public enum RiskLevel {

    SAFE,

    /** 보호자에게 알리되 송금은 진행한다. */
    CAUTION,

    /** 보호자 승인 전까지 보류한다(transactions.HELD). */
    DANGER
}
