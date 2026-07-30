package com.paywith.payment.dto;

import lombok.Getter;

@Getter
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
}
