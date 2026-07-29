package com.paywith.fds.domain;

/**
 * 송금 FDS 위험 등급. risk_evaluations.risk_level 과 이름이 일치한다.
 *
 * <p>처리 단계(transactions.status)와는 별개 축이다. SAFE/CAUTION 은 동일하게 송금이 진행되며,
 * 주의 판정을 받았다는 사실은 이 값으로만 구분된다.
 */
public enum RiskLevel {

    /** 정상 송금. */
    SAFE,

    /** 보호자에게 알림을 보내되 송금은 그대로 진행한다. */
    CAUTION,

    /** 보호자 승인 전까지 송금을 보류한다(transactions.HELD). */
    DANGER
}
