package com.paywith.charge.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@ApiModel(description = "충전 내역 목록의 한 건")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeHistoryItem {
    @ApiModelProperty(value = "충전 거래 ID", example = "999")
    private Long transactionId;

    @ApiModelProperty(value = "충전 대상 피보호자 ID", example = "1")
    private Long wardId;

    @ApiModelProperty(value = "충전 대상 피보호자 이름", example = "김시니어")
    private String wardName;

    @ApiModelProperty(value = "충전 금액(원)", example = "30000")
    private Long amount;

    @ApiModelProperty(value = "충전 처리 시각(DB 값, 오프셋·소수초 없음)", example = "2026-07-16T15:32:00")
    private LocalDateTime createdAt;
}
