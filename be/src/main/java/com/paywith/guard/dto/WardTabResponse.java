package com.paywith.guard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@ApiModel(description = "보호자 홈 상단의 피보호자 탭 한 건")
@Getter
public class WardTabResponse {

    @ApiModelProperty(value = "피보호자 회원 ID. wardId 쿼리 파라미터로 다시 넘기면 그 피보호자가 선택된다", example = "1")
    private final Long wardId;

    @ApiModelProperty(value = "피보호자 이름", example = "김시니어")
    private final String name;

    @ApiModelProperty(value = "피보호자 아바타 번호(1~6)", example = "1")
    private final Integer avatarId;

    @ApiModelProperty(value = "이 피보호자에게 승인 대기(PENDING·미만료) 건이 하나라도 있으면 true. "
        + "어느 탭이 선택됐는지와 무관하게 모든 탭에 계산된다", example = "true")
    private final boolean hasPending;

    public WardTabResponse(Long wardId, String name, Integer avatarId, boolean hasPending) {
        this.wardId = wardId;
        this.name = name;
        this.avatarId = avatarId;
        this.hasPending = hasPending;
    }
}
