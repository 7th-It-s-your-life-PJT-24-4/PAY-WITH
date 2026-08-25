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
    @ApiModelProperty(value = "은행 표준코드(GET /api/banks 의 bankCode). 서버 검증 없음 — 누락·미등록 코드는 500",
        required = true, example = "004")
    private String bankCode;

    @ApiModelProperty(value = "계좌번호(하이픈 없는 숫자만). 형식·필수 서버 검증 없음 — 누락 시 500",
        required = true, example = "11012300006781")
    private String accountNo;

    @ApiModelProperty(value = "계좌 비밀번호(실서비스 연동 대비 수집 — 데모에서는 미사용, 실명조회는 가입 시 저장된 "
        + "생년월일로 수행). 서버 검증 없음 — 어떤 값을 보내도 읽지 않는다",
        required = true, example = "1234")
    private String accountPassword;
}
