package com.paywith.guard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Getter;

@ApiModel(description = "보호자 홈 응답. 연동된 피보호자 탭 목록과 선택된 피보호자의 잔액·승인대기·최근거래 요약")
@Getter
public class GuardHomeResponse {

    @ApiModelProperty(value = "연동(ACTIVE)된 피보호자 탭 목록. 연동 시각(connected_at) 오름차순. "
        + "연동된 피보호자가 없으면 빈 배열")
    private final List<WardTabResponse> wards;

    @ApiModelProperty(value = "선택된 피보호자 요약. wardId 를 생략하면 가장 먼저 연동된 피보호자가 선택된다. "
        + "연동된 피보호자가 없으면 null")
    private final SelectedWardResponse selectedWard;

    public GuardHomeResponse(List<WardTabResponse> wards, SelectedWardResponse selectedWard) {
        this.wards = wards;
        this.selectedWard = selectedWard;
    }
}
