package com.paywith.guard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@ApiModel(description = "보호자 홈의 승인 대기 거래 한 건")
@Getter
public class PendingApprovalResponse {

    @ApiModelProperty(value = "대상 거래 ID", example = "88")
    private final Long transactionId;

    @ApiModelProperty(value = "거래 금액(원)", example = "30000")
    private final Long amount;

    @ApiModelProperty(value = "수취인 예금주명. 거래에 수취인 정보가 없으면 null", example = "박지연")
    private final String holderName;

    @ApiModelProperty(value = "수취 계좌번호. 거래에 수취인 정보가 없으면 null", example = "110-234-567890")
    private final String accountNo;

    @ApiModelProperty(value = "위험 평가 총점(0~100, risk_evaluations.total_score). 평가 기록이 없으면 null", example = "85")
    private final Integer riskScore;

    @ApiModelProperty(value = "위험 평가에서 가장 높은 점수를 낸 룰의 설명(risk_rules.description). "
        + "평가 기록이나 발동 룰이 없으면 null", example = "매우 큰 금액을 송금했어요.")
    private final String riskReason;

    @ApiModelProperty(value = "승인 요청 생성 시각(approval_requests.requested_at). 오프셋 없는 ISO-8601",
        example = "2026-07-16T15:22:07")
    private final LocalDateTime createdAt;

    public PendingApprovalResponse(
        Long transactionId,
        Long amount,
        String holderName,
        String accountNo,
        Integer riskScore,
        String riskReason,
        LocalDateTime createdAt
    ) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.holderName = holderName;
        this.accountNo = accountNo;
        this.riskScore = riskScore;
        this.riskReason = riskReason;
        this.createdAt = createdAt;
    }
}
