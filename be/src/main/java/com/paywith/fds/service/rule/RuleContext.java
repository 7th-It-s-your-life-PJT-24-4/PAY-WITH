package com.paywith.fds.service.rule;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 룰·단축평가가 참조하는 판정 입력. 수집 단계에서 한 번에 만들어지고 이후로는 DB를 다시 보지 않는다.
 */
@Getter
@Builder
public class RuleContext {

    private final BigDecimal amount;
    private final String memo;
    private final LocalDateTime requestedAt;

    /** 0이면 신규 수취인. */
    private final int recipientSendCount;

    /** 등록 주체(본인/보호자)는 구분하지 않는다. */
    private final boolean recipientRegisteredSafe;

    private final int recentTransferCount;
    private final int recentDistinctRecipientCount;
    private final boolean recipientRejectedBefore;

    /** 외부 조회 실패 시 false(fail-open). */
    private final boolean recipientReportedAsFraud;

    private final boolean pendingApprovalExists;
    private final List<String> memoKeywords;
}
