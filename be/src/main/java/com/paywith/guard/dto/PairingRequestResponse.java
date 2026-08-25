package com.paywith.guard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@ApiModel(description = "페어링 확인 요청 생성 결과")
@Getter
public class PairingRequestResponse {
    @ApiModelProperty(value = "확인 요청 식별자(UUID). 상태 조회와 보호자 승인에 쓰며 생성 후 2분간 유효",
            example = "3f29c1e0-8b7a-4c2d-9e1f-5a6b7c8d9e0f")
    private final String requestId;

    public PairingRequestResponse(String requestId) {
        this.requestId = requestId;
    }
}