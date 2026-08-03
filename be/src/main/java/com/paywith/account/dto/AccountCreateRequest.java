package com.paywith.account.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "계좌 등록 요청")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreateRequest {
    @ApiModelProperty(value = "은행 표준코드", required = true, example = "004")
    private String bankCode;

    @ApiModelProperty(value = "계좌번호", required = true, example = "11012300006781")
    private String accountNo;

    @ApiModelProperty(value = "계좌 비밀번호(실명조회용)", required = true, example = "1234")
    private String accountPassword;
}
