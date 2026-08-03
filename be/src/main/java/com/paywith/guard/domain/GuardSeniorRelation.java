package com.paywith.guard.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuardSeniorRelation {

    private Long relationId;
    private Long guardId;
    private Long wardId;
    private String status;
    private LocalDateTime connectedAt;
}