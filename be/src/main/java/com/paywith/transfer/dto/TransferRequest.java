package com.paywith.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    private String bankCode;
    private String accountNo;
    private Long amount;
    private String memo;
    private String transferPin;
}