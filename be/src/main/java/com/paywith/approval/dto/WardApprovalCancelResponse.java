package com.paywith.approval.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@ApiModel(description = "피보호자 승인 대기 취소 결과")
@Getter
public class WardApprovalCancelResponse {

    @ApiModelProperty(value = "승인요청 ID", example = "1")
    private final Long approvalId;

    @ApiModelProperty(value = "대상 송금 거래 ID", example = "500")
    private final Long transactionId;

    @ApiModelProperty(value = "종결 상태", example = "CANCELED")
    private final String status;

    @ApiModelProperty(value = "피보호자가 취소한 시각")
    private final LocalDateTime canceledAt;

    public WardApprovalCancelResponse(
        Long approvalId, Long transactionId, LocalDateTime canceledAt) {
        this.approvalId = approvalId;
        this.transactionId = transactionId;
        this.status = "CANCELED";
        this.canceledAt = canceledAt;
    }
}
