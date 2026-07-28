package com.paywith.fds.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 송금 API가 FDS 평가를 요청할 때 넘기는 입력.
 * 이체 이력·수취인 위험 정보 조회는 FDS 내부에서 수행하므로 식별자와 요청 값만 받는다.
 */
@Getter
@RequiredArgsConstructor
public class FdsEvaluationRequest {

    private final Long walletId;
    private final Long recipientId;
    private final BigDecimal amount;
    private final String memo;
}
