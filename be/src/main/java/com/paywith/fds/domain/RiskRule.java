package com.paywith.fds.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RiskRule {

    private Long ruleId;
    private String ruleCode;
    private String description;
    private Integer score;
    private boolean active;
}
