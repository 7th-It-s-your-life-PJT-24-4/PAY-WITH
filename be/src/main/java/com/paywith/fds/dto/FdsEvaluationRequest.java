package com.paywith.fds.dto;

import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;

/** 송금 API 가 FDS 에 넘기는 입력. 이력 조회는 FDS 가 알아서 하므로 식별자와 요청 값만 받는다. */
@Getter
public class FdsEvaluationRequest {

    /** status=REQUESTED 로 이미 생성된 거래. 속도 룰 집계에서 이 거래를 빼는 데 쓴다. */
    private final Long transactionId;

    private final Long walletId;
    private final Long recipientId;
    private final BigDecimal amount;
    private final String memo;

    /** 컨트롤러를 거치지 않아 @Valid 가 안 걸리므로 여기서 막는다. */
    public FdsEvaluationRequest(
        Long transactionId,
        Long walletId,
        Long recipientId,
        BigDecimal amount,
        String memo
    ) {
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId는 필수입니다.");
        this.walletId = Objects.requireNonNull(walletId, "walletId는 필수입니다.");
        this.recipientId = Objects.requireNonNull(recipientId, "recipientId는 필수입니다.");
        this.amount = Objects.requireNonNull(amount, "amount는 필수입니다.");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount는 0보다 커야 합니다: " + amount);
        }
        this.memo = memo;
    }
}
