package com.paywith.charge.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@ApiModel(description = "충전 요청")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChargeRequest {
    @ApiModelProperty(value = "충전에 사용할 연동 계좌 ID", required = true, example = "1")
    @NotNull(message = "연동 계좌 ID는 필수입니다.")
    private Long accountId;

    @ApiModelProperty(value = "충전 금액(원)", required = true, example = "30000")
    @NotNull(message = "충전 금액은 필수입니다.")
    @Positive(message = "충전 금액은 0보다 커야 합니다.")
    private Long amount;

    @ApiModelProperty(value = "충전 비밀번호 (보호자 대리충전 시 필수, 본인 충전 시 불필요)", example = "123456")
    private String pin;
}
