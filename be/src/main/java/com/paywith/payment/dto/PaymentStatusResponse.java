package com.paywith.payment.dto;

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

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public String getStatus() {
        return status;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public Long getAmount() {
        return amount;
    }

    public String getPaidAt() {
        return paidAt;
    }

    public Long getRemainingBalance() {
        return remainingBalance;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public String getExpiresAt() {
        return expiresAt;
    }
}
