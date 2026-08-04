package com.paywith.safeaccount.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "안전계좌 등록 요청 (보호자)")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardSafeAccountRegisterRequest {
    @ApiModelProperty(value = "은행 표준코드", required = true, example = "004")
    private String bankCode;

    @ApiModelProperty(value = "계좌번호", required = true, example = "11012300006781")
    private String accountNo;

    @ApiModelProperty(value = "안전계좌 별칭. 50자를 초과하면 400", example = "용돈용")
    private String accountAlias;
}
