package com.paywith.safeaccount.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafeAccountListItem {
    private Long safeAccountId;
    private Long recipientId;
    private String bankCode;
    private String bankName;
    private String accountNo;
    private String holderName;
    private String accountAlias;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}
