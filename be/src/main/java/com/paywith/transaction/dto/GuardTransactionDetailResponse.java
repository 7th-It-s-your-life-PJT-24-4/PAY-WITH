package com.paywith.transaction.dto;

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
    private String type;
    private String direction;
    private String status;
    private String riskLevel;
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