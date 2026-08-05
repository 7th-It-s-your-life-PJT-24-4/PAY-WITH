package com.paywith.bank.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Bank {

    private String bankCode;
    private String bankName;
    private boolean active;
}