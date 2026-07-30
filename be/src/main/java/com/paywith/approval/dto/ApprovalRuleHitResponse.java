package com.paywith.approval.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 보호자가 보는 값이라 rule_id 는 내보내지 않는다. */
@ApiModel(description = "보류 사유 한 건")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalRuleHitResponse {

    @ApiModelProperty(value = "룰 코드", example = "SUSPICIOUS_MEMO")
    private String ruleCode;

    @ApiModelProperty(value = "룰 설명", example = "메모에 위험 키워드 포함")
    private String description;

    @ApiModelProperty(value = "적용 점수. 블랙리스트 확정 건은 0", example = "25")
    private Integer score;
}
