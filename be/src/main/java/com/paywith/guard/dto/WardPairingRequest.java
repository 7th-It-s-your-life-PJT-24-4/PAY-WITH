package com.paywith.guard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@ApiModel(description = "페어링 코드 제출 요청 (즉시 연결·확인 요청 생성 공용)")
@Getter
@Setter
public class WardPairingRequest {

    @ApiModelProperty(value = "보호자가 발급한 인증 코드(숫자 5자리). Bean Validation 없이 서비스에서 검사하며, "
        + "누락·형식 불일치는 400 PAIRING_001", required = true, example = "72941")
    private String pairingCode;
}