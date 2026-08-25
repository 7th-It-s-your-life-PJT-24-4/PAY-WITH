package com.paywith.transfer.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "수취인 실명조회 결과")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipientInquiryResponse {
    @ApiModelProperty(value = "은행 표준코드 (요청값을 그대로 반환)", example = "004")
    private String bankCode;

    @ApiModelProperty(value = "은행명 (banks 테이블 조회, 미등록 코드면 \"알 수 없는 은행\")", example = "KB국민은행")
    private String bankName;

    @ApiModelProperty(value = "계좌번호 (요청값을 그대로 반환)", example = "11012300006781")
    private String accountNo;

    @ApiModelProperty(value = "조회된 예금주명 (현재 실명조회 목 구현 — 계좌번호 자릿수 조합으로 생성된 시연용 이름)", example = "홍길동")
    private String recipientName;
}