package com.paywith.bank.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@ApiModel(description = "은행 목록 조회 응답의 한 건 (GET /api/banks)")
@Getter
@AllArgsConstructor
public class BankResponse {

    @ApiModelProperty(value = "금융결제원 표준 은행코드(banks.bank_code). 숫자 3자리 문자열이며 계좌 등록(POST /api/accounts)의 bankCode 로 "
        + "그대로 보낸다. 활성 은행(is_active = TRUE)만 이 값 오름차순(ORDER BY bank_code ASC)으로 내려가며 현재 시드 22개 은행 모두 활성",
        example = "004")
    private final String bankCode;

    @ApiModelProperty(value = "은행 표시명(banks.bank_name)", example = "KB국민은행")
    private final String bankName;
}
