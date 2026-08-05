package com.paywith.payment.fds.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 이동 속도 단축평가의 기준점 — 같은 지갑의 24시간 내 최신 COMPLETED 결제.
 * 좌표는 NULL일 수 있다(좌표 미등록 가맹점 이력). 기준점에 좌표가 없으면 속도 계산 없이
 * 스킵하며, 더 오래된 좌표 보유 행으로 기준점을 대체하지 않는다.
 */
@Getter
@Setter
public class LastCompletedPayment {

    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime completedAt;
}
