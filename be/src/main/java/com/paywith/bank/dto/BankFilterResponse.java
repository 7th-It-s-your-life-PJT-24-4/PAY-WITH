package com.paywith.bank.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Getter;

@ApiModel(description = "계좌번호 기반 은행 필터링 응답")
@Getter
public class BankFilterResponse {

    @ApiModelProperty(value = "계좌번호 형식 규칙(BankAccountRule: 은행별 자릿수 범위 + 일부 은행의 앞자리 prefix)과 일치한 활성 은행 후보. "
        + "일치 은행이 없으면 null 이 아닌 빈 배열. 순서는 DB 반환 순서(ORDER BY 없음)라 정렬 보장 없음 — 실질적으로는 bank_code 오름차순. "
        + "규칙이 정의되지 않은 은행코드는 제외되나 현재 시드 22개 은행 모두 규칙 보유")
    private final List<BankCandidateResponse> banks;

    public BankFilterResponse(List<BankCandidateResponse> banks) {
        this.banks = banks;
    }
}