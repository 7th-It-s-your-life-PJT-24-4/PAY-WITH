package com.paywith.approval.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 승인 화면에 필요한 값을 한 번에 모은 조회 결과.
 * 여러 테이블에 걸쳐 있어 approval_requests 행과 1:1로 대응하지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ApprovalRequestView {

    private Long approvalId;
    private Long transactionId;
    private LocalDateTime requestedAt;
    private LocalDateTime expiredAt;

    private String seniorName;

    private BigDecimal amount;
    private String memo;

    /** 수취인 정보. 거래에 recipient_id 가 없으면 null 이 된다. */
    private String recipientName;
    private String bankName;
    private String accountNo;

    /** 평가 기록이 없으면 null. */
    private String riskLevel;
    private Integer totalScore;
}
