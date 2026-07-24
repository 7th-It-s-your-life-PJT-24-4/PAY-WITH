package com.paywith.charge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeResponse {
    private Long transactionId;
    private Long chargeAmount;
    private Long balanceAfter;
    private String bankName;
    private String maskedAccountNo;
    private LocalDateTime createdAt;
}
