package com.paywith.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {
    private IdempotencyStatus status;
    private String idempotencyKey;
    private TransferResponse response;
}
