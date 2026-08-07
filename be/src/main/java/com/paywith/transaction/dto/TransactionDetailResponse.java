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

@ApiModel(description = "거래내역 상세")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDetailResponse {

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
    private LocalDateTime occurredAt;
    private Long balanceAfter;

    // Mapper 조회 시점엔 riskScore로 채워졌다가,
    // Service가 riskAnalysis 객체로 재조립하면서 이 필드는 버려짐
    private Integer riskScore;

    private RiskAnalysisResponse riskAnalysis;
}