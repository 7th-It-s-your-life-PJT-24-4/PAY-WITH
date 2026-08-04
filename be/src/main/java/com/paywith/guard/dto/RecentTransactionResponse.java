package com.paywith.guard.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecentTransactionResponse {

    private Long transactionId;
    private String type;
    private String status;
    private String counterpartyName;
    private Long amount;
    private String riskLevel;
    private String riskReason;
    private LocalDateTime createdAt;
}