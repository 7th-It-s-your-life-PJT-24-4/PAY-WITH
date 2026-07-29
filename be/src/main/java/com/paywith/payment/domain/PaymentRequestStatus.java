package com.paywith.payment.domain;

/**
 * QR 결제 요청 상태 (payment_requests.status와 1:1).
 * PENDING=생성·미스캔 / PROCESSING=스캔됨 / COMPLETED=차감 완료
 * FAILED=잔액 부족·FDS 차단 / EXPIRED=60초 경과 / CANCELED=피보호자 취소
 */
public enum PaymentRequestStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    EXPIRED,
    CANCELED
}
