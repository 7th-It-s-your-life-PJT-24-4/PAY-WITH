package com.paywith.transaction.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    private Long transactionId;
    private Long walletId;
    private Long recipientId;
    private Long merchantId;
    private Long accountId;
    private String type;           // CHARGE, PAYMENT, TRANSFER_OUT
    private Long initiatedBy;      // 보호자 충전 시 보호자 user_id, NULL이면 본인
    private Long amount;
    private String memo;
    private Long balanceAfter;
    private String status;         // REQUESTED, HELD, APPROVED, REJECTED, COMPLETED, CANCELED, BLOCKED
    private Double latitude;
    private Double longitude;
    private String pgPaymentKey;
    private Integer riskScore;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
