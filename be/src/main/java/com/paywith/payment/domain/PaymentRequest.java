package com.paywith.payment.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequest {

    private Long paymentId;
    private Long walletId;
    private Long seniorId;
    private String qrToken;
    private PaymentRequestStatus status;
    private Long transactionId;
    private Long merchantId;
    private Long amount;
    private String failureCode;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 조회 전용 (merchants / transactions LEFT JOIN 결과 — INSERT/UPDATE에는 사용하지 않음)
    private String merchantName;
    private LocalDateTime paidAt;
    private Long remainingBalance;
}
