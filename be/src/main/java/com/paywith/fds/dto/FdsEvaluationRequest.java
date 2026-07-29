package com.paywith.fds.dto;

import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;

/**
 * 송금 API가 FDS 평가를 요청할 때 넘기는 입력.
 * 이체 이력·수취인 위험 정보 조회는 FDS 내부에서 수행하므로 식별자와 요청 값만 받는다.
 */
@Getter
public class FdsEvaluationRequest {

    /**
     * 평가 대상 거래. status=REQUESTED 로 이미 생성된 행이어야 한다.
     *
     * <p>속도 관련 룰(REPEATED / DIVISION_TRANSFER / BL_RAPID_NEW_RECIPIENT)의 집계에서 이 거래를
     * 제외하는 데 쓴다. 거래 행을 FDS 평가보다 먼저 만들기 때문에, 제외하지 않으면 방금 만든 행이
     * "최근 송금 이력"에 포함되어 임계값이 1씩 느슨해진다.
     */
    private final Long transactionId;

    private final Long walletId;
    private final Long recipientId;
    private final BigDecimal amount;
    private final String memo;

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
