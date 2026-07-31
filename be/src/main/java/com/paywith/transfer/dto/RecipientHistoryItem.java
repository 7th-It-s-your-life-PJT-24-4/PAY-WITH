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

public class RecipientHistoryItem {
    private Long recipientId;
    private String holderName;
    private String bankCode;
    private String bankName;
    private String accountNo;
    private LocalDateTime lastSentAt;
    private Integer sendCount;
    private Boolean isRegisteredSafe;
    private Long safeAccountId;
    private String accountAlias;

}
