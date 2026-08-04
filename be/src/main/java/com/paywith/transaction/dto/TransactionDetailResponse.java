package com.paywith.transaction.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@ApiModel(description = "거래내역 상세")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDetailResponse {

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
    private LocalDateTime occurredAt;
    private Long balanceAfter;

    // Mapper 조회 시점엔 평면값(riskScore/analyzedAt)으로 채워졌다가,
    // Service가 riskAnalysis 객체로 재조립하면서 이 두 필드는 버려진다.
    private Integer riskScore;
    private LocalDateTime analyzedAt;

    private RiskAnalysisResponse riskAnalysis;
}