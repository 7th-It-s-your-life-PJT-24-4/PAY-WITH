package com.paywith.account.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "연동 계좌 정보")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {
    @ApiModelProperty(value = "연동 계좌 ID", example = "1")
    private Long accountId;

    @ApiModelProperty(value = "은행 표준코드", example = "004")
    private String bankCode;

    @ApiModelProperty(value = "은행명", example = "KB국민은행")
    private String bankName;

    @ApiModelProperty(value = "계좌번호", example = "11012300006781")
    private String accountNo;
}
