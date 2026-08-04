package com.paywith.transaction.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@ApiModel(description = "거래내역 목록의 한 건")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistoryItem {

    @ApiModelProperty(value = "거래 번호", example = "141")
    private Long transactionId;

    @ApiModelProperty(value = "거래 유형", allowableValues = "CHARGE,TRANSFER,PAYMENT", example = "PAYMENT")
    private String type;

    @ApiModelProperty(value = "잔액 증감 방향", allowableValues = "IN,OUT", example = "OUT")
    private String direction;

    @ApiModelProperty(value = "거래 내역에 표시할 이름", example = "이마트 서울점")
    private String title;

    @ApiModelProperty(value = "거래 금액의 절댓값", example = "45200")
    private Long amount;

    @ApiModelProperty(value = "거래 처리 상태",
            allowableValues = "REQUESTED,HELD,APPROVED,PROCESSING,REJECTED,COMPLETED,CANCELED,BLOCKED,FAILED",
            example = "COMPLETED")
    private String status;

    @ApiModelProperty(value = "거래 위험 단계. 평가 기록이 없으면 null",
            allowableValues = "SAFE,CAUTION,DANGER", example = "SAFE")
    private String riskLevel;

    @ApiModelProperty(value = "목록에 표시할 거래 시각")
    private LocalDateTime occurredAt;
}