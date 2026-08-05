package com.paywith.transaction.dto;

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

    private Long transactionId;
    private String type;
    private String status;
    private String counterpartyName;
    private Long amount;
    private String riskLevel;
    private String riskReason;
    private LocalDateTime createdAt;
}