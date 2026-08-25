package com.paywith.payment.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@ApiModel(description = "QR 결제 실행 결과(가맹점 스캐너용). 완료 건에만 내려가며, 같은 qrToken 재요청은 이 본문을 그대로 재응답한다(멱등)")
@Getter
public class ExecuteResponse {

    @ApiModelProperty(value = "원장 거래 번호(transactions.transaction_id). 완료 결제에만 발급되며 결제 요청(paymentId)과 1:1로 연결",
        example = "141")
    private final Long transactionId;

    @ApiModelProperty(value = "결제 상태. 이 응답은 완료 건에만 내려가므로 항상 \"COMPLETED\""
        + "(잔액 부족 422·FDS 차단 403·무효 토큰 400은 오류 응답으로 내려가 이 본문이 없음)",
        allowableValues = "COMPLETED", example = "COMPLETED")
    private final String status;

    @ApiModelProperty(value = "결제 금액(원). 요청 amount가 그대로 확정된 값이며, 멱등 재응답은 DB payment_requests.amount",
        example = "15000")
    private final Long amount;

    @ApiModelProperty(value = "가맹점명(merchants.name). 요청 merchantId로 서버가 조회한 값", example = "한마음경로식당")
    private final String merchantName;

    @ApiModelProperty(value = "결제 완료 시각(ISO 8601, +09:00 오프셋). 최초 완료 응답은 서버 시각 기반이라 분수초(최대 마이크로초)가 "
        + "붙을 수 있고, 멱등 재응답은 DB transactions.completed_at(DATETIME) 기반이라 분수초 없이 초 단위로 내려간다",
        example = "2026-08-25T10:00:00.123456+09:00")
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
