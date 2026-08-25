package com.paywith.approval.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 보호자가 보는 값이라 rule_id 와 룰별 점수는 내보내지 않는다.
 *
 * <p>룰별 점수를 노출하면 응답 몇 건으로 배점 테이블을 역산할 수 있고, 그러면 위험 임계값
 * 아래로 맞추는 송금 설계가 가능해진다. 크기 감각은 합산값(totalScore)만으로 전달하며,
 * 근거의 우선순위는 점수 대신 배열 순서(점수 큰 순)로 표현한다.
 */
@ApiModel(description = "보류 사유 한 건. 배열 순서가 근거의 우선순위다")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalRuleHitResponse {

    @ApiModelProperty(value = "룰 코드", example = "SUSPICIOUS_MEMO")
    private String ruleCode;

    @ApiModelProperty(value = "룰 설명(risk_rules.description 그대로)",
        example = "송금 메모에 검찰·수사·대출 등 주의가 필요한 표현이 있어요.")
    private String description;
}
