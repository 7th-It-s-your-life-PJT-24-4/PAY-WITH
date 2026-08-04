package com.paywith.charge.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@ApiModel(description = "보호자 충전 내역 상세")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ChargeDetailResponse {
    @ApiModelProperty(value = "충전 거래 ID", example = "999")
    private Long transactionId;

    @ApiModelProperty(value = "충전 대상 피보호자 ID", example = "1")
    private Long wardId;

    @ApiModelProperty(value = "충전 대상 피보호자 이름", example = "김시니어")
    private String wardName;

    @ApiModelProperty(value = "충전 금액(원)", example = "30000")
    private Long amount;

    @ApiModelProperty(value = "메모")
    private String memo;

    @ApiModelProperty(value = "출금에 사용한 연동 계좌 ID", example = "1")
    private Long accountId;

    @ApiModelProperty(value = "출금 계좌 정보")
    private ChargeAccountInfo account;

    @ApiModelProperty(value = "충전 처리 시각")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "충전 후 지갑 잔액(원)", example = "80000")
    private Long balanceAfter;
}
