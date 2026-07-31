package com.paywith.approval.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 승인 직후 이어서 실행한 송금의 결과.
 *
 * <p>승인 자체의 성공/실패({@link ApprovalDecisionResponse#getStatus()})와 분리해서 전달한다.
 * 승인은 기록이 확정되면 끝이지만 송금은 잔액이나 외부 입금에서 따로 실패할 수 있고, 그때
 * 예외를 그대로 던지면 보호자에게는 "승인이 실패했다"로 읽혀 재시도(409)를 유발한다.
 *
 * <p>여기의 {@code status} 는 이번 송금 시도의 결과이지 transactions.status 의 복사본이 아니다.
 * 예를 들어 잔액 부족으로 실패하면 거래는 APPROVED 로 남고 이 값만 FAILED 가 된다.
 */
@ApiModel(description = "승인 후 송금 실행 결과")
@Getter
public class TransferResultResponse {

    @ApiModelProperty(value = "송금 시도 결과", example = "COMPLETED", allowableValues = "COMPLETED,FAILED")
    private final String status;

    @ApiModelProperty(value = "실패 사유. 성공이면 null", example = "송금 가능한 잔액이 부족합니다.")
    private final String failureReason;

    @ApiModelProperty(value = "송금 완료 시각. 실패면 null")
    private final LocalDateTime completedAt;

    @ApiModelProperty(value = "송금 후 지갑 잔액. 실패면 null", example = "120000")
    private final Long balanceAfter;

    private TransferResultResponse(
        String status, String failureReason, LocalDateTime completedAt, Long balanceAfter) {
        this.status = status;
        this.failureReason = failureReason;
        this.completedAt = completedAt;
        this.balanceAfter = balanceAfter;
    }

    public static TransferResultResponse completed(LocalDateTime completedAt, Long balanceAfter) {
        return new TransferResultResponse("COMPLETED", null, completedAt, balanceAfter);
    }

    public static TransferResultResponse failed(String failureReason) {
        return new TransferResultResponse("FAILED", failureReason, null, null);
    }
}
