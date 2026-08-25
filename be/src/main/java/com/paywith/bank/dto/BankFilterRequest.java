package com.paywith.bank.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@ApiModel(description = "계좌번호 기반 은행 필터링 요청")
@Getter
@Setter
public class BankFilterRequest {

    @ApiModelProperty(value = "은행을 추정할 계좌번호. 하이픈 없는 숫자 1~20자리(정규식 \\d{1,20})만 허용하며 누락·비숫자·자릿수 이탈은 "
        + "400 ACCOUNT_001 \"계좌번호 형식이 올바르지 않습니다.\". Bean Validation 은 없고 서비스에서 검사하며 "
        + "역할(403 AUTH_004)·페어링(403 WARD_001) 검사가 먼저다. 형식이 맞아도 일치 은행이 없으면 400 이 아니라 200 + 빈 배열",
        required = true, example = "1002123456789")
    private String accountNo;
}