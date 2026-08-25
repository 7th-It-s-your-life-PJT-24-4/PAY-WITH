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
    @ApiModelProperty(value = "은행 표준코드(banks 테이블에 등록된 코드). 서버 검증 없음: 누락·미등록 코드는 DB 제약 위반으로 500", required = true, example = "004")
    private String bankCode;

    @ApiModelProperty(value = "계좌번호. 서버 검증 없음: 숫자 형식 검사 없음, 누락 시 DB 제약 위반으로 500", required = true, example = "11012300006781")
    private String accountNo;

    @ApiModelProperty(value = "안전계좌 별칭(최대 50자). 50자를 초과하면 400 SAFE_ACCOUNT_004, null·공백만이면 null 로 저장. 재등록 시 요청값으로 덮어씀", example = "용돈용")
    private String accountAlias;
}
