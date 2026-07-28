package com.paywith.account.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    private Long accountId;
    private Long userId;
    private String bankCode;
    private String accountNo;
    private String holderName;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}
