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
    @ApiModelProperty(value = "은행 표준코드 (서버 검증 없음 — 누락·미등록 코드여도 200, bankName 은 \"알 수 없는 은행\")",
        required = true, example = "004")
    private String bankCode;

    @ApiModelProperty(value = "계좌번호 (서버 검증 없음 — 누락·5자리 미만이어도 200, 예금주명은 \"홍길동\")",
        required = true, example = "11012300006781")
    private String accountNo;
}