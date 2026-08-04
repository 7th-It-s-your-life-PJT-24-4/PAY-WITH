package com.paywith.guard.dto;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class PendingApprovalResponse {

    private final Long transactionId;
    private final Long amount;
    private final String holderName;
    private final String accountNo;
    private final Integer riskScore;
    private final String riskReason;
    private final LocalDateTime createdAt;

    public PendingApprovalResponse(
        Long transactionId,
        Long amount,
        String holderName,
        String accountNo,
        Integer riskScore,
        String riskReason,
        LocalDateTime createdAt
    ) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.holderName = holderName;
        this.accountNo = accountNo;
        this.riskScore = riskScore;
        this.riskReason = riskReason;
        this.createdAt = createdAt;
    }
}