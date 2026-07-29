package com.paywith.charge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeHistoryItem {
    private Long transactionId;
    private Long wardId;
    private String wardName;
    private Long amount;
    private LocalDateTime createdAt;
}
