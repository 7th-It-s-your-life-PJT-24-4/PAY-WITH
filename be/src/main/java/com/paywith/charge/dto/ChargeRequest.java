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
    @ApiModelProperty(value = "충전에 사용할 연동 계좌 ID. 호출자(본인 충전은 피보호자, 보호자 충전은 보호자) 소유여야 하며 "
        + "아니면 404 ACCOUNT_004. 누락 시 400 REQUEST_001", required = true, example = "1")
    @NotNull(message = "연동 계좌 ID는 필수입니다.")
    private Long accountId;

    @ApiModelProperty(value = "충전 금액(원). 1 이상 정수 — 누락·0 이하면 400 REQUEST_001", required = true, example = "30000")
    @NotNull(message = "충전 금액은 필수입니다.")
    @Positive(message = "충전 금액은 0보다 커야 합니다.")
    private Long amount;

    @ApiModelProperty(value = "충전 비밀번호(가입 시 등록한 숫자 6자리 결제 비밀번호). 보호자 대리충전에서만 대조하며 "
        + "누락·불일치면 400 CHARGE_003. 형식 검증은 없고 본인 충전 경로에서는 무시된다", example = "123456")
    private String pin;
}
