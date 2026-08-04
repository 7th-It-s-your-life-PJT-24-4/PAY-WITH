package com.paywith.charge.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "충전 요청")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChargeRequest {
    @ApiModelProperty(value = "충전에 사용할 연동 계좌 ID", required = true, example = "1")
    private Long accountId;

    @ApiModelProperty(value = "충전 금액(원)", required = true, example = "30000")
    private Long amount;
}
