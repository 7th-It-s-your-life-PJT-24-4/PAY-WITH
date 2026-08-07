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

    private Long transactionId;
    private TransactionCategory type;
    private TransactionStatus status;
    private String counterpartyName;
    private Long amount;
    private RiskLevel riskLevel;
    private String riskReason;
    private LocalDateTime createdAt;
}