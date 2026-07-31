package com.paywith.transfer.dto;

import lombok.*;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor

public class TransferExecutionContext {
    private Long transactionId;
    private Long walletId;
    private Long userId;
    private String bankCode;
    private String bankName;
    private String accountNo;
    private String holderName;
    private Long amount;
    private String memo;
}