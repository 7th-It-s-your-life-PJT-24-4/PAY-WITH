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

@ApiModel(description = "피보호자 거래내역 상세 (보호자용)")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardTransactionDetailResponse {

    private Long transactionId;
    private TransactionCategory type;
    private String direction;
    private TransactionStatus status;
    private RiskLevel riskLevel;
    private String counterpartyName;
    private String bankName;
    private String accountNo;
    private Long amount;
    private String memo;
    private Long balanceAfter;

    private Integer riskScore;

    private RiskAnalysisResponse riskAnalysis;
    private LocalDateTime occurredAt;
}