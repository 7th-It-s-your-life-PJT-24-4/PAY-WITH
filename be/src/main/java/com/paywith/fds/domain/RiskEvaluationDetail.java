package com.paywith.fds.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RiskEvaluationDetail {

    private Long detailId;
    private Long evaluationId;
    private Long ruleId;
    private Integer score;
}
