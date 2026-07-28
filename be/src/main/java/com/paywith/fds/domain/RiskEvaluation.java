package com.paywith.fds.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RiskEvaluation {

    private Long evaluationId;
    private Long transactionId;
    private Integer totalScore;
    private Integer threshold;
    private boolean held;
    private LocalDateTime evaluatedAt;
}
