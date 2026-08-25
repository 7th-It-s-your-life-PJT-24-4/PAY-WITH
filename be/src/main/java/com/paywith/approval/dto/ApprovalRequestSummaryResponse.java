package com.paywith.approval.dto;

import com.paywith.approval.domain.ApprovalRequestView;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

@ApiModel(description = "승인요청 목록의 한 건")
@Getter
public class ApprovalRequestSummaryResponse {

    @ApiModelProperty(value = "승인요청 ID", example = "1")
    private final Long approvalId;

    @ApiModelProperty(value = "대상 거래 ID", example = "500")
    private final Long transactionId;

    @ApiModelProperty(value = "송금을 요청한 피보호자 ID. 목록을 피보호자별로 묶거나 필터링할 때 쓴다", example = "42")
    private final Long wardId;

    @ApiModelProperty(value = "송금을 요청한 피보호자 이름", example = "김시니어")
    private final String wardName;

    @ApiModelProperty(value = "송금 금액(원)", example = "2000000")
    private final BigDecimal amount;

    @ApiModelProperty(value = "수취인 예금주명", example = "박수취")
    private final String holderName;

    @ApiModelProperty(value = "수취 은행", example = "신한은행")
    private final String bankName;

    @ApiModelProperty(value = "위험 등급. 평가 기록이 없으면 null", example = "DANGER",
        allowableValues = "SAFE,CAUTION,DANGER")
    private final String riskLevel;

    @ApiModelProperty(value = "승인요청 생성 시각")
    private final LocalDateTime requestedAt;

    @ApiModelProperty(value = "승인 만료 시각. 대기 목록에서는 이 시각이 지나면 제외된다")
    private final LocalDateTime expiredAt;

    @ApiModelProperty(value = "승인요청 상태. 대기 목록은 항상 PENDING, 이력은 조회한 status 와 같다. "
        + "CANCELED 는 현재 구현상 응답에 나오지 않는다", example = "APPROVED",
        allowableValues = "PENDING,APPROVED,REJECTED,CANCELED,EXPIRED")
    private final String status;

    @ApiModelProperty(value = "보호자 응답 시각 또는 자동 만료 시각(EXPIRED 는 expiredAt 과 같은 값). 대기 건이면 null. "
        + "피보호자 취소는 현재 기록되지 않는다")
    private final LocalDateTime respondedAt;

    public ApprovalRequestSummaryResponse(ApprovalRequestView view) {
        this.approvalId = view.getApprovalId();
        this.transactionId = view.getTransactionId();
        this.wardId = view.getWardId();
        this.wardName = view.getWardName();
        this.amount = view.getAmount();
        this.holderName = view.getHolderName();
        this.bankName = view.getBankName();
        this.riskLevel = view.getRiskLevel();
        this.requestedAt = view.getRequestedAt();
        this.expiredAt = view.getExpiredAt();
        this.status = view.getStatus();
        this.respondedAt = view.getRespondedAt();
    }
}
