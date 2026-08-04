package com.paywith.transfer.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "송금 요청")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    @ApiModelProperty(value = "은행 표준코드", required = true, example = "004")
    private String bankCode;

    @ApiModelProperty(value = "계좌번호", required = true, example = "11012300006781")
    private String accountNo;

    @ApiModelProperty(value = "송금 금액(원)", required = true, example = "50000")
    private Long amount;

    @ApiModelProperty(value = "송금 메모", example = "생활비")
    private String memo;

    @ApiModelProperty(value = "송금 비밀번호", required = true, example = "123456")
    private String transferPin;
}