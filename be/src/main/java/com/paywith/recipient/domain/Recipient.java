package com.paywith.recipient.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recipient {
    private Long recipientId;
    private Long wardId;
    private String bankCode;
    private String accountNo;
    private String holderName;
    private Integer sendCount;
    private LocalDateTime firstSentAt;
    private LocalDateTime lastSentAt;
    private Boolean isRegisteredSafe;
    private LocalDateTime safeRegisteredAt;
    private Long safeRegisteredBy;
    private String accountAlias;
}