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

    @ApiModelProperty(value = "처리 후 승인요청 상태", example = "APPROVED",
        allowableValues = "APPROVED,REJECTED")
    private final String status;

    @ApiModelProperty(value = "처리 시각. 서버 시각(LocalDateTime.now()) 기준이라 마이크로초까지 포함될 수 있다",
        example = "2026-07-31T13:42:26.123456")
    private final LocalDateTime respondedAt;

    @ApiModelProperty(value = "승인 후 이어서 실행한 송금의 결과. 승인이면 실패(FAILED)까지 포함해 항상 담기고, 거절이면 null")
    private final TransferResultResponse transfer;

    public ApprovalDecisionResponse(
        Long approvalId, Long transactionId, String status, LocalDateTime respondedAt) {
        this(approvalId, transactionId, status, respondedAt, null);
    }

    private ApprovalDecisionResponse(
        Long approvalId,
        Long transactionId,
        String status,
        LocalDateTime respondedAt,
        TransferResultResponse transfer) {
        this.approvalId = approvalId;
        this.transactionId = transactionId;
        this.status = status;
        this.respondedAt = respondedAt;
        this.transfer = transfer;
    }

    /**
     * 승인 결과는 그대로 두고 송금 결과만 덧붙인 새 응답을 만든다.
     *
     * <p>승인 처리(트랜잭션 안)와 송금 실행(트랜잭션 밖)이 분리돼 있어, 승인 서비스는 송금
     * 결과를 모르는 채로 응답을 만든다. 두 결과를 합치는 일은 둘을 순서대로 호출하는
     * {@link com.paywith.approval.service.ApprovalTransferFacade} 가 맡는다.
     */
    public ApprovalDecisionResponse withTransfer(TransferResultResponse transfer) {
        return new ApprovalDecisionResponse(
            approvalId, transactionId, status, respondedAt, transfer);
    }
}
