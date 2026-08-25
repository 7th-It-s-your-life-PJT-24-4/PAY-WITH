package com.paywith.guard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@ApiModel(description = "보호자 홈의 최근 거래 한 건")
@Getter
@Setter
public class RecentTransactionResponse {

    @ApiModelProperty(value = "거래 ID", example = "88")
    private Long transactionId;

    @ApiModelProperty(value = "거래 종류. DB 의 TRANSFER_OUT 은 TRANSFER 로 바꿔 내보낸다",
        example = "TRANSFER", allowableValues = "CHARGE,PAYMENT,TRANSFER")
    private String type;

    @ApiModelProperty(value = "거래 상태. 홈에는 종결 상태만 나온다",
        example = "COMPLETED", allowableValues = "COMPLETED,REJECTED,CANCELED")
    private String status;

    @ApiModelProperty(value = "상대방 표시명. CHARGE=출금 연동계좌 은행명, PAYMENT=가맹점명, TRANSFER=수취인 예금주명",
        example = "박지연")
    private String counterpartyName;

    @ApiModelProperty(value = "거래 금액(원)", example = "30000")
    private Long amount;

    @ApiModelProperty(value = "거래 위험 단계. 평가 기록이 없으면 null",
        example = "DANGER", allowableValues = "SAFE,CAUTION,DANGER")
    private String riskLevel;

    @ApiModelProperty(value = "위험 평가에서 가장 높은 점수를 낸 룰의 설명(risk_rules.description). "
        + "평가 기록이나 발동 룰이 없으면 null", example = "매우 큰 금액을 송금했어요.")
    private String riskReason;

    @ApiModelProperty(value = "거래 생성 시각(transactions.created_at). 오프셋 없는 ISO-8601", example = "2026-07-16T15:30:00")
    private LocalDateTime createdAt;
}
