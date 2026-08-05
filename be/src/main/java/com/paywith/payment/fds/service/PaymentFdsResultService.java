package com.paywith.payment.fds.service;

import com.paywith.fds.dto.FdsDecision;

/**
 * 결제 FDS 판정 결과 저장. 실행 경로가 거래 행(COMPLETED 또는 BLOCKED)을 만든 뒤
 * 같은 트랜잭션 안에서 호출한다. 송금과 달리 DANGER 여도 승인 요청을 만들지 않는다 —
 * 결제 DANGER 는 즉시 거절(D1)이라 보호자 승인 절차가 없다.
 */
public interface PaymentFdsResultService {

    void save(Long transactionId, FdsDecision decision);
}
