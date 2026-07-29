package com.paywith.charge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ChargeAccountInfo {
    private String bankCode;
    private String bankName;
    private String accountNo;
}
