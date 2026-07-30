package com.paywith.payment.dto;

import lombok.Getter;

@Getter
public class PaymentCancelResponse {

    private final Long paymentId;
    private final String status;
    private final String canceledAt;

    public PaymentCancelResponse(Long paymentId, String status, String canceledAt) {
        this.paymentId = paymentId;
        this.status = status;
        this.canceledAt = canceledAt;
    }
}
