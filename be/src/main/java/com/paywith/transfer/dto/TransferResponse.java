package com.paywith.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {
    private Long transactionId;
    private String status;
    private String holderName;      // recipientName → holderName (DB: recipients.holder_name)
    private String bankCode;
    private String bankName;
    private String accountNo;
    private Long amount;
    private String memo;
    private LocalDateTime completedAt;
    private Long balanceAfter;
}