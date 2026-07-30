package com.paywith.approval.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@ApiModel(description = "승인/거절 처리 결과")
@Getter
public class ApprovalDecisionResponse {

    @ApiModelProperty(value = "승인요청 ID", example = "1")
    private final Long approvalId;

    @ApiModelProperty(value = "대상 송금 거래 ID", example = "500")
    private final Long transactionId;

    @ApiModelProperty(value = "처리 후 승인요청 상태", example = "APPROVED", allowableValues = "APPROVED,REJECTED")
    private final String status;

    @ApiModelProperty(value = "처리 시각")
    private final LocalDateTime respondedAt;

    public ApprovalDecisionResponse(
        Long approvalId, Long transactionId, String status, LocalDateTime respondedAt) {
        this.approvalId = approvalId;
        this.transactionId = transactionId;
        this.status = status;
        this.respondedAt = respondedAt;
    }
}
