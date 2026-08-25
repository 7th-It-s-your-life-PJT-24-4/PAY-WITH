package com.paywith.guard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@ApiModel(description = "보호자에게 온 확정 대기중(PENDING) 페어링 확인 요청. 요청이 없거나 만료·처리됐으면 data 자체가 null")
@Getter
public class PendingPairingRequestResponse {
    @ApiModelProperty(value = "확인 요청 식별자(UUID). 확인 요청 승인의 경로 변수로 쓰며 생성 후 2분간 유효",
            example = "3f29c1e0-8b7a-4c2d-9e1f-5a6b7c8d9e0f")
    private final String requestId;
    @ApiModelProperty(value = "요청을 보낸 피보호자 이름(users.name, 최대 50자, null 아님)", example = "김영희")
    private final String wardName;
    @ApiModelProperty(value = "피보호자 전화번호 마스킹값. 앞 (길이-8)자리 + **** + 뒤 4자리(01012345678 → 010****5678). "
            + "번호가 8자 미만이면 마스킹 없이 원문", example = "010****5678")
    private final String wardPhoneMasked;

    public PendingPairingRequestResponse(String requestId, String wardName, String wardPhoneMasked) {
        this.requestId = requestId;
        this.wardName = wardName;
        this.wardPhoneMasked = wardPhoneMasked;
    }
}