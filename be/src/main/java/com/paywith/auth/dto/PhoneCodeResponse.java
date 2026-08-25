package com.paywith.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@ApiModel(description = "인증번호 발송 결과")
@Getter
public class PhoneCodeResponse {

    @ApiModelProperty(value = "발송한 인증번호의 유효 시간(초). 항상 300", example = "300")
    private final int expireIn;

    public PhoneCodeResponse(int expireIn) {
        this.expireIn = expireIn;
    }
}