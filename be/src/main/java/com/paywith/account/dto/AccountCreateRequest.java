package com.paywith.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreateRequest {
    private String bankCode;
    private String accountNo;
    private String accountPassword;
    private String birthDate;
}
