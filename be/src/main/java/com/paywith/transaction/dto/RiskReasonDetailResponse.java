package com.paywith.transaction.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "위험 판단 사유 한 건")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskReasonDetailResponse {

    @ApiModelProperty(value = "위험 판단 사유 코드", example = "HIGH_AMOUNT")
    private String ruleCode;

    @ApiModelProperty(value = "사용자에게 표시할 위험 요소", example = "평소 이용 패턴과 다른 고액 송금 시도")
    private String description;

    @ApiModelProperty(value = "해당 항목의 위험 점수", example = "30")
    private Integer score;
}