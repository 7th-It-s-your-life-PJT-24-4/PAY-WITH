package com.paywith.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BankResponse {

    private final String bankCode;
    private final String bankName;
}
