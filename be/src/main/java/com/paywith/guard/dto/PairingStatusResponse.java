package com.paywith.guard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@ApiModel(description = "페어링 확인 요청 상태")
@Getter
public class PairingStatusResponse {
    @ApiModelProperty(value = "요청 상태. 만료(생성 후 2분)·없는 요청은 EXPIRED, CONFIRMED는 승인 후 30초 동안만 유지",
            allowableValues = "PENDING,CONFIRMED,EXPIRED", example = "PENDING")
    private final String status;

    public PairingStatusResponse(String status) {
        this.status = status;
    }
}