package com.paywith.payment.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@ApiModel(description = "결제 요청 상태(폴링용). 상태별로 null인 필드가 다르다 — 스캔 전(PENDING·EXPIRED·CANCELED)은 merchantName·amount가 null, "
    + "COMPLETED만 transactionId·paidAt·remainingBalance가 채워지고, FAILED만 failureCode·failureMessage가 채워진다")
@Getter
public class PaymentStatusResponse {

    @ApiModelProperty(value = "결제 요청 번호(payment_requests.payment_id)", example = "42")
    private final Long paymentId;

    @ApiModelProperty(value = "원장 거래 번호(transactions.transaction_id). COMPLETED만 값이 있고 그 외 상태는 null"
        + "(FDS 차단 FAILED는 BLOCKED 거래 행이 생기지만 결제 요청에 연결하지 않아 null)",
        example = "141")
    private final Long transactionId;

    @ApiModelProperty(value = "결제 요청 상태. PENDING=생성·미스캔, PROCESSING=스캔됨, COMPLETED=차감 완료, FAILED=잔액 부족·FDS 차단, "
        + "EXPIRED=60초 경과, CANCELED=피보호자 취소. PENDING이면서 만료 시각이 지난 건은 조회 시점에 EXPIRED로 전이된다",
        allowableValues = "PENDING,PROCESSING,COMPLETED,FAILED,EXPIRED,CANCELED", example = "COMPLETED")
    private final String status;

    @ApiModelProperty(value = "가맹점명(merchants.name). 스캔 시 확정되므로 PROCESSING·COMPLETED·FAILED만 값이 있고 "
        + "PENDING·EXPIRED·CANCELED는 null",
        example = "한마음경로식당")
    private final String merchantName;

    @ApiModelProperty(value = "결제 금액(원). 스캔 시 확정되므로 PROCESSING·COMPLETED·FAILED만 값이 있고 PENDING·EXPIRED·CANCELED는 null",
        example = "15000")
    private final Long amount;

    @ApiModelProperty(value = "결제 완료 시각(transactions.completed_at, ISO 8601 +09:00 오프셋). COMPLETED만 값이 있고 그 외 null. "
        + "DB DATETIME 기반이라 분수초 없음",
        example = "2026-08-25T10:00:00+09:00")
    private final String paidAt;

    @ApiModelProperty(value = "결제 후 지갑 잔액(원, transactions.balance_after). COMPLETED만 값이 있고 그 외 null"
        + "(FAILED는 잔액 차감이 없어 null)",
        example = "137000")
    private final Long remainingBalance;

    @ApiModelProperty(value = "실패 사유 코드. FAILED만 값이 있고 그 외 null. INSUFFICIENT_BALANCE=잔액 부족, FDS_BLOCKED=이상거래 차단",
        allowableValues = "INSUFFICIENT_BALANCE,FDS_BLOCKED", example = "INSUFFICIENT_BALANCE")
    private final String failureCode;

    @ApiModelProperty(value = "실패 사유 문구. INSUFFICIENT_BALANCE는 \"결제 가능한 잔액이 부족합니다.\", FDS_BLOCKED는 \"결제에 실패했습니다.\""
        + "(범용 문구 — 결제 실행 403의 차단 문구와 다름). failureCode가 null이면 null",
        example = "결제 가능한 잔액이 부족합니다.")
    private final String failureMessage;

    @ApiModelProperty(value = "토큰 만료 시각(생성 시각 + 60초, ISO 8601 +09:00 오프셋). 모든 상태에서 항상 값이 있으며 "
        + "DB DATETIME 기반이라 분수초 없음",
        example = "2026-08-25T10:01:00+09:00")
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
