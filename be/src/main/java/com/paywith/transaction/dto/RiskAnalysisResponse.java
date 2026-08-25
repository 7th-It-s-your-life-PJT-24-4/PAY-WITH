package com.paywith.transaction.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

import java.util.List;

@ApiModel(description = "위험 분석 결과. type 이 CHARGE 이거나 위험 평가 기록(risk_evaluations)이 없으면 이 객체 자체가 null. "
        + "평가 기록이 있으면 riskLevel 이 SAFE 여도 채워진다")
@Getter
public class RiskAnalysisResponse {

    @ApiModelProperty(value = "위험 분석 점수(0~100, risk_evaluations.total_score)", example = "88")
    private final int riskScore;

    @ApiModelProperty(value = "LLM이 판단 근거를 요약한 자연어 설명(llm_risk_reviews.reason, DB 최대 1000자, 서버 길이 검증 없음). "
            + "사후 재검토 기록이 없으면 null — 현재는 이 테이블에 기록하는 코드가 없어 항상 null", example = "평소보다 큰 금액이...")
    private final String summary;

    @ApiModelProperty(value = "위험 판단 사유 코드 배열(risk_rules.rule_code), 점수 높은 순. "
            + "발동한 룰이 없으면 빈 배열. 한국어 문구 매핑은 프론트 책임", example = "[\"REPEATED\", \"NEW_RECIPIENT\"]")
    private final List<String> reasons;

    public RiskAnalysisResponse(int riskScore, String summary, List<String> reasons) {
        this.riskScore = riskScore;
        this.summary = summary;
        this.reasons = reasons;
    }
}