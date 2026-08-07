package com.paywith.guard.dto;

import com.paywith.guard.domain.GuardSummary;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@ApiModel(description = "시니어가 조회하는 보호자 정보")
@Getter
public class GuardInfoResponse {

    @ApiModelProperty(value = "보호자 이름", example = "김보호")
    private final String name;

    @ApiModelProperty(value = "보호자 전화번호", example = "01012345678")
    private final String phone;

    @ApiModelProperty(value = "보호자 아바타 번호(1~6)", example = "1")
    private final Integer avatarId;

    public GuardInfoResponse(GuardSummary guard) {
        this.name = guard.getName();
        this.phone = guard.getPhone();
        this.avatarId = guard.getAvatarId();
    }
}