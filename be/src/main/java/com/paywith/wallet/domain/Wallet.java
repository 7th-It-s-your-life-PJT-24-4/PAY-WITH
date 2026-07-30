package com.paywith.wallet.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {
    private Long walletId;
    private Long userId;
    private Long balance;
    private String status;
    private String pin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}