package com.paywith.transaction.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@ApiModel(description = "위험 분석 결과. 안전한 거래이거나 평가 대상이 아니면 이 객체 자체가 null")
@Getter
public class RiskAnalysisResponse {

    @ApiModelProperty(value = "위험 분석 점수(0~100)", example = "88")
    private final int riskScore;

    @ApiModelProperty(value = "LLM이 판단 근거를 요약한 짧은 자연어 설명. "
            + "완료된 거래 중 사후 재검토 대상이었던 경우에만 존재", example = "평소보다 큰 금액이...")
    private final String summary;

    @ApiModelProperty(value = "상세 위험 판단 근거")
    private final List<RiskReasonDetailResponse> reasons;

    @ApiModelProperty(value = "위험 분석 시각")
    private final LocalDateTime analyzedAt;

    public RiskAnalysisResponse(
            int riskScore, String summary, List<RiskReasonDetailResponse> reasons, LocalDateTime analyzedAt
    ) {
        this.riskScore = riskScore;
        this.summary = summary;
        this.reasons = reasons;
        this.analyzedAt = analyzedAt;
    }
}