package com.paywith.payment.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@ApiModel(description = "결제 요청 취소 결과. 이미 취소된 건의 재취소도 409가 아니라 같은 본문을 200으로 재응답한다(멱등)")
@Getter
public class PaymentCancelResponse {

    @ApiModelProperty(value = "결제 요청 번호(payment_requests.payment_id)", example = "42")
    private final Long paymentId;

    @ApiModelProperty(value = "취소 후 상태. PENDING을 방금 취소한 건이든 이미 취소된 건의 재취소든 항상 \"CANCELED\""
        + "(PROCESSING·COMPLETED·FAILED·EXPIRED는 409 오류로 이 본문이 내려가지 않음)",
        allowableValues = "CANCELED", example = "CANCELED")
    private final String status;

    @ApiModelProperty(value = "취소 시각(payment_requests.updated_at, ISO 8601 +09:00 오프셋). DB DATETIME 기반이라 분수초 없음. "
        + "재취소 멱등 응답에서는 최초 취소 시각",
        example = "2026-08-25T10:00:30+09:00")
    private final String canceledAt;

    public PaymentCancelResponse(Long paymentId, String status, String canceledAt) {
        this.paymentId = paymentId;
        this.status = status;
        this.canceledAt = canceledAt;
    }
}
