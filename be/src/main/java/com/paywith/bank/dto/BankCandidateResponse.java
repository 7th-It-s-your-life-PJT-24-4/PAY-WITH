package com.paywith.bank.dto;

import lombok.Getter;

@Getter
public class BankCandidateResponse {

    private final String bankCode;
    private final String bankName;

    public BankCandidateResponse(String bankCode, String bankName) {
        this.bankCode = bankCode;
        this.bankName = bankName;
    }
}