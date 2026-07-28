package com.paywith.external.openbanking.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WithdrawResponse {
    private String rspCode;
    private String rspMessage;
    private String bankTranId;
    private Long tranAmt;

    public boolean isSuccess() {
        return "A0000".equals(rspCode);
    }
}