package com.paywith.charge.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@ApiModel(description = "보호자 충전 내역 목록")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeHistoryListResponse {
    @ApiModelProperty(value = "충전 내역 목록")
    private List<ChargeHistoryItem> charges;
}
