package com.paywith.safeaccount.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardSafeAccountRegisterRequest {
    private String bankCode;
    private String accountNo;
    private String accountAlias;
}
