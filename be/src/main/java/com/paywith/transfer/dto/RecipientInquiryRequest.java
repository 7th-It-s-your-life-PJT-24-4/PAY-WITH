package com.paywith.transfer.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "수취인 실명조회 요청")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipientInquiryRequest {
    @ApiModelProperty(value = "은행 표준코드", required = true, example = "004")
    private String bankCode;

    @ApiModelProperty(value = "계좌번호", required = true, example = "11012300006781")
    private String accountNo;
}