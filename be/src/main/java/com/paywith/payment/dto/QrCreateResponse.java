package com.paywith.payment.dto;

public class QrCreateResponse {

    private final Long paymentId;
    private final String qrToken;
    private final Long availableBalance;
    private final String expiresAt;
    private final int expiresInSeconds;

    public QrCreateResponse(
        Long paymentId,
        String qrToken,
        Long availableBalance,
        String expiresAt,
        int expiresInSeconds
    ) {
        this.paymentId = paymentId;
        this.qrToken = qrToken;
        this.availableBalance = availableBalance;
        this.expiresAt = expiresAt;
        this.expiresInSeconds = expiresInSeconds;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public String getQrToken() {
        return qrToken;
    }

    public Long getAvailableBalance() {
        return availableBalance;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public int getExpiresInSeconds() {
        return expiresInSeconds;
    }
}
