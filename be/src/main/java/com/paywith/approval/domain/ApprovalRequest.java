package com.paywith.approval.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApprovalRequest {

    private Long approvalId;
    private Long transactionId;
    private String status;
    private Long respondedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime expiredAt;
}
