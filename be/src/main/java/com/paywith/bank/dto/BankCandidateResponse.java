package com.paywith.bank.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@ApiModel(description = "은행 필터링 후보 한 건")
@Getter
public class BankCandidateResponse {

    @ApiModelProperty(value = "금융결제원 표준 은행코드(banks.bank_code). 숫자 3자리 문자열로 GET /api/banks 의 bankCode 와 같은 값이며 "
        + "계좌 등록(POST /api/accounts)의 bankCode 로 그대로 보낸다", example = "004")
    private final String bankCode;

    @ApiModelProperty(value = "은행 표시명(banks.bank_name)", example = "KB국민은행")
    private final String bankName;

    public BankCandidateResponse(String bankCode, String bankName) {
        this.bankCode = bankCode;
        this.bankName = bankName;
    }
}