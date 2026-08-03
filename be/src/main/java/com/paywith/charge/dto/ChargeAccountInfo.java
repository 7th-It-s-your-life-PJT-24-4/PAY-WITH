package com.paywith.charge.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "충전에 사용된 출금 계좌 정보")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ChargeAccountInfo {
    @ApiModelProperty(value = "은행 표준코드", example = "004")
    private String bankCode;

    @ApiModelProperty(value = "은행명", example = "KB국민은행")
    private String bankName;

    @ApiModelProperty(value = "계좌번호", example = "11012300006781")
    private String accountNo;
}
