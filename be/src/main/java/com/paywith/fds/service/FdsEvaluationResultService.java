package com.paywith.fds.service;

import com.paywith.fds.dto.FdsDecision;

/**
 * 판정 결과 저장. FDS 내부에서만 쓰이고 송금 API 는 이 존재를 모른다.
 *
 * <p>{@link FdsEvaluationServiceImpl} 과 별도 빈이어야 한다. 같은 클래스 안에서 호출하면
 * 프록시를 거치지 않아 {@code @Transactional} 이 적용되지 않는다.
 */
public interface FdsEvaluationResultService {

    void save(Long transactionId, FdsDecision decision);
}
