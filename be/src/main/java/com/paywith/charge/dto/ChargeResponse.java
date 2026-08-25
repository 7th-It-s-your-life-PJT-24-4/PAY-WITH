package com.paywith.charge.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@ApiModel(description = "충전 결과")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeResponse {
    @ApiModelProperty(value = "충전 거래 ID", example = "999")
    private Long transactionId;

    @ApiModelProperty(value = "충전 금액(원)", example = "30000")
    private Long chargeAmount;

    @ApiModelProperty(value = "충전 후 지갑 잔액(원)", example = "80000")
    private Long balanceAfter;

    @ApiModelProperty(value = "출금 은행명", example = "KB국민은행")
    private String bankName;

    @ApiModelProperty(value = "출금 계좌번호", example = "11012300006781")
    private String accountNo;

    @ApiModelProperty(value = "충전 처리 시각. 요청 시점의 now() 원본이라 오프셋 없이 소수초가 붙을 수 있다",
        example = "2026-07-16T15:32:00.123456")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "충전 대상 피보호자 ID. 보호자 충전(chargeByGuard) 응답에서만 채워지고 본인 충전 응답에서는 null",
        example = "1")
    private Long wardId;

    @ApiModelProperty(value = "충전 대상 피보호자 이름. 보호자 충전(chargeByGuard) 응답에서만 채워지고 본인 충전 응답에서는 null",
        example = "김시니어")
    private String wardName;
}
