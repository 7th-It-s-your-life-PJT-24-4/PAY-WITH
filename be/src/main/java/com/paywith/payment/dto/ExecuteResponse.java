package com.paywith.payment.dto;

import lombok.Getter;

@Getter
public class ExecuteResponse {

    private final Long transactionId;
    private final String status;
    private final Long amount;
    private final String merchantName;
    private final String createdAt;

    public ExecuteResponse(
        Long transactionId,
        String status,
        Long amount,
        String merchantName,
        String createdAt
    ) {
        this.transactionId = transactionId;
        this.status = status;
        this.amount = amount;
        this.merchantName = merchantName;
        this.createdAt = createdAt;
    }
}
