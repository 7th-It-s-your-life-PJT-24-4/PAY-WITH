package com.paywith.transaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.transaction.domain.TransactionCategory;
import com.paywith.transaction.domain.TransactionStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@ApiModel(description = "피보호자 거래내역 상세 (보호자용)")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardTransactionDetailResponse {

    @ApiModelProperty(value = "거래 번호(transactions.transaction_id)", example = "141")
    private Long transactionId;
    @ApiModelProperty(value = "거래 유형(TRANSFER_OUT 은 TRANSFER 로 변환)", allowableValues = "CHARGE,TRANSFER,PAYMENT", example = "PAYMENT")
    private TransactionCategory type;

    @ApiModelProperty(value = "잔액 증감 방향. CHARGE 만 IN, PAYMENT·TRANSFER 는 OUT", allowableValues = "IN,OUT", example = "OUT")
    private String direction;

    @ApiModelProperty(value = "거래 상태(transactions.status). 상태 필터 없이 원장 값을 그대로 반환", example = "COMPLETED")
    private TransactionStatus status;

    @ApiModelProperty(value = "거래 위험 단계. 평가 기록이 없으면 null", allowableValues = "SAFE,CAUTION,DANGER", example = "CAUTION")
    private RiskLevel riskLevel;

    @ApiModelProperty(value = "상대방 표시명. CHARGE=피보호자(지갑 소유자) 본인 이름, PAYMENT=가맹점명, TRANSFER=수취인 예금주명",
            example = "박지연")
    private String counterpartyName;

    @ApiModelProperty(value = "은행명. CHARGE=출금 연동계좌 은행, TRANSFER=수취 계좌 은행, PAYMENT=null", example = "신한은행")
    private String bankName;

    @ApiModelProperty(value = "계좌번호. CHARGE=출금 연동계좌 번호, TRANSFER=수취 계좌번호, PAYMENT=null", example = "110-234-567890")
    private String accountNo;

    @ApiModelProperty(value = "거래 금액의 절댓값(원)", example = "45200")
    private Long amount;
    @ApiModelProperty(value = "메모. TRANSFER 는 송금 시 입력한 메모(없으면 null), CHARGE 는 항상 null(충전 흐름이 memo 를 저장하지 않음), PAYMENT 는 null", example = "생활비")
    private String memo;

    @ApiModelProperty(value = "거래 후 잔액. 완료(COMPLETED) 시점에 채워지며 완료되지 않은 거래는 null", example = "154800")
    private Long balanceAfter;

    @JsonIgnore
    private Integer riskScore;

    @ApiModelProperty(value = "위험 분석 결과. type 이 CHARGE 이거나 위험 평가 기록이 없으면 null(SAFE 라도 평가 기록이 있으면 채워짐)")
    private RiskAnalysisResponse riskAnalysis;

    @ApiModelProperty(value = "거래 시각. completed_at 이 있으면 그 값, 없으면 created_at. 오프셋 없는 ISO-8601",
            example = "2026-07-16T15:02:10")
    private LocalDateTime occurredAt;
}