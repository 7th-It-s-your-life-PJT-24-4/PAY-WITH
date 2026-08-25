package com.paywith.transaction.dto;

import com.paywith.fds.domain.RiskLevel;
import com.paywith.transaction.domain.TransactionCategory;
import com.paywith.transaction.domain.TransactionStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@ApiModel(description = "보호자용 거래내역 목록의 한 건")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardTransactionHistoryItem {

    @ApiModelProperty(value = "거래 번호(transactions.transaction_id)", example = "141")
    private Long transactionId;
    @ApiModelProperty(value = "거래 유형(TRANSFER_OUT 은 TRANSFER 로 변환)", allowableValues = "CHARGE,TRANSFER,PAYMENT", example = "PAYMENT")
    private TransactionCategory type;
    @ApiModelProperty(value = "거래 상태(transactions.status). 상태 필터 없이 원장 값을 그대로 반환", example = "COMPLETED")
    private TransactionStatus status;

    @ApiModelProperty(value = "상대방 표시명. CHARGE=출금 연동계좌 은행명, PAYMENT=가맹점명, TRANSFER=수취인 예금주명",
            example = "박지연")
    private String counterpartyName;

    @ApiModelProperty(value = "거래 금액의 절댓값(원)", example = "45200")
    private Long amount;

    @ApiModelProperty(value = "거래 위험 단계. 평가 기록이 없으면 null", allowableValues = "SAFE,CAUTION,DANGER", example = "DANGER")
    private RiskLevel riskLevel;

    @ApiModelProperty(value = "위험 평가에서 가장 높은 점수를 낸 룰의 설명(risk_rules.description). "
            + "평가 기록이나 발동 룰이 없으면 null", example = "처음 송금하는 계좌예요.")
    private String riskReason;

    @ApiModelProperty(value = "거래 생성 시각(transactions.created_at). 오프셋 없는 ISO-8601", example = "2026-07-16T15:30:00")
    private LocalDateTime createdAt;
}