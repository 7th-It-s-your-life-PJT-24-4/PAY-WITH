package com.paywith.payment.fds.service;

import com.paywith.fds.dto.FdsDecision;
import com.paywith.merchant.domain.Merchant;

/**
 * 결제 FDS 판정 진입점. 수집·판정만 하고 저장은 하지 않는다 — 판정은 실행 경로의
 * 잠금(payment_requests FOR UPDATE) 이전에, 저장({@link PaymentFdsResultService})은
 * 거래 행이 확정된 뒤 트랜잭션 안에서 일어나야 하므로 송금과 달리 두 단계를 분리한다.
 */
public interface PaymentFdsEvaluationService {

    /** @param merchant 실행 경로가 이미 조회한 가맹점 — 여기서 재조회하지 않는다 */
    FdsDecision evaluate(Long walletId, Long amount, Merchant merchant);
}
