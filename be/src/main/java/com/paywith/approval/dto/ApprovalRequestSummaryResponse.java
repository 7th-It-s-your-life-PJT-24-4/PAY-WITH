package com.paywith.approval.dto;

import com.paywith.approval.domain.ApprovalRequestView;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

@ApiModel(description = "승인 대기 목록의 한 건")
@Getter
public class ApprovalRequestSummaryResponse {

    @ApiModelProperty(value = "승인요청 ID", example = "1")
    private final Long approvalId;

    @ApiModelProperty(value = "송금을 요청한 시니어 ID. 목록을 시니어별로 묶거나 필터링할 때 쓴다", example = "42")
    private final Long seniorId;

    @ApiModelProperty(value = "송금을 요청한 시니어 이름", example = "김시니어")
    private final String seniorName;

    @ApiModelProperty(value = "송금 금액(원)", example = "2000000")
    private final BigDecimal amount;

    @ApiModelProperty(value = "수취인 예금주명", example = "박수취")
    private final String holderName;

    @ApiModelProperty(value = "수취 은행", example = "신한은행")
    private final String bankName;

    @ApiModelProperty(value = "위험 등급", example = "DANGER", allowableValues = "SAFE,CAUTION,DANGER")
    private final String riskLevel;

    @ApiModelProperty(value = "승인요청 생성 시각")
    private final LocalDateTime requestedAt;

    @ApiModelProperty(value = "승인 만료 시각. 지나면 목록에서 빠진다")
    private final LocalDateTime expiredAt;

    public ApprovalRequestSummaryResponse(ApprovalRequestView view) {
        this.approvalId = view.getApprovalId();
        this.seniorId = view.getSeniorId();
        this.seniorName = view.getSeniorName();
        this.amount = view.getAmount();
        this.holderName = view.getHolderName();
        this.bankName = view.getBankName();
        this.riskLevel = view.getRiskLevel();
        this.requestedAt = view.getRequestedAt();
        this.expiredAt = view.getExpiredAt();
    }
}
