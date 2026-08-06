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
    private String status;
    private LocalDateTime respondedAt;

    private Long wardId;
    private String wardName;

    /**
     * 거래 종류(transactions.type). 현재 승인 대기는 송금(TRANSFER_OUT)에서만 생성되지만,
     * 결제 승인 경로가 생기면 화면이 종류를 구분해야 하므로 조회 시점부터 들고 있는다.
     */
    private String type;

    private BigDecimal amount;
    private String memo;

    /** 수취인 정보. 거래에 recipient_id 가 없으면 null 이 된다. 이름은 recipients.holder_name 을 따른다. */
    private String holderName;
    private String bankName;
    private String accountNo;

    /** 평가 기록이 없으면 null. */
    private String riskLevel;
    private Integer totalScore;
}
