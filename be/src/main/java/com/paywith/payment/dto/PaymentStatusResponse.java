package com.paywith.payment.dto;

import lombok.Getter;

@Getter
public class PaymentStatusResponse {

    private final Long paymentId;
    private final Long transactionId;
    private final String status;
    private final String merchantName;
    private final Long amount;
    private final String paidAt;
    private final Long remainingBalance;
    private final String failureCode;
    private final String failureMessage;
    private final String expiresAt;

    public PaymentStatusResponse(
        Long paymentId,
        Long transactionId,
        String status,
        String merchantName,
        Long amount,
        String paidAt,
        Long remainingBalance,
        String failureCode,
        String failureMessage,
        String expiresAt
    ) {
        this.paymentId = paymentId;
        this.transactionId = transactionId;
        this.status = status;
        this.merchantName = merchantName;
        this.amount = amount;
        this.paidAt = paidAt;
        this.remainingBalance = remainingBalance;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.expiresAt = expiresAt;
    }
}
