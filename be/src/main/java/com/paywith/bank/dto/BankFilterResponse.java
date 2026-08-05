package com.paywith.bank.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class BankFilterResponse {

    private final List<BankCandidateResponse> banks;

    public BankFilterResponse(List<BankCandidateResponse> banks) {
        this.banks = banks;
    }
}